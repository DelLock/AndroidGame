package com.example.androidgame.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.androidgame.data.api.OpenMeteoApi
import com.example.androidgame.data.api.dto.CurrentWeatherDto
import com.example.androidgame.data.api.dto.toDomain
import com.example.androidgame.domain.model.ALL_BASES
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.DroneBase
import com.example.androidgame.domain.model.DroneGameEngine
import com.example.androidgame.domain.model.DroneTelemetry
import com.example.androidgame.domain.model.NORMAL_COLLECT_COST
import com.example.androidgame.domain.model.WINDY_COLLECT_COST
import com.example.androidgame.domain.model.baseFor
import com.example.androidgame.domain.model.startDrones
import com.example.androidgame.util.retryIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class DroneRepository(
    private val openMeteoApi: OpenMeteoApi,
    context: Context? = null,
    mainContext: CoroutineContext = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(mainContext + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences? = context?.getSharedPreferences("drone_hub_storage", Context.MODE_PRIVATE)

    var credits by mutableIntStateOf(prefs?.getInt("saved_credits", 300) ?: 300)
        private set

    var drones: List<Drone> by mutableStateOf(loadSavedDrones())
        private set

    var lastDeletedDrone: Drone? by mutableStateOf(null)
        private set

    var undoTimerSeconds by mutableIntStateOf(0)
        private set

    var missionMessage: String? by mutableStateOf(null)
        private set

    private var missionNoticeTimerSeconds = 0

    // Видит ли система интернет (обновляется из DroneApp через ConnectivityMonitor).
    private var systemOnline by mutableStateOf(true)

    // false, если последний запрос провалился из-за сети (нет ответа сервера).
    private var serverReachable by mutableStateOf(true)

    /** Растёт при каждом возвращении сети: по нему карточка сбрасывает лимит повторов. */
    var networkRestoreCount by mutableIntStateOf(0)
        private set

    val isOnline: Boolean get() = systemOnline && serverReachable

    fun onSystemNetworkChanged(online: Boolean) {
        if (online && !systemOnline) {
            serverReachable = true
            networkRestoreCount++
        }
        systemOnline = online
    }

    private class WeatherRequest(val deferred: Deferred<CurrentWeatherDto>) {
        var waiters = 0
    }

    private val cityWeatherCache = mutableMapOf<String, CachedWeather>()
    private val inFlight = mutableMapOf<String, WeatherRequest>()

    private val unhandledCompletedMissions = mutableListOf<String>()
    private var totalPendingReward = 0

    private var undoJob: Job? = null
    private var missionNoticeJob: Job? = null
    private var nextId = (drones.maxOfOrNull { it.id } ?: 0) + 1

    init {
        loadSavedWeatherCache()
        refreshWeatherForActiveMissions()
        scope.launch {
            while (isActive) {
                delay(1.seconds)
                tickMissions()
            }
        }
    }

    fun drone(id: Int): Drone? = drones.find { it.id == id }

    private fun loadSavedDrones(): List<Drone> {
        if (prefs == null || !prefs.contains("saved_drones_list")) return startDrones()
        val savedJson = prefs.getString("saved_drones_list", null) ?: return startDrones()
        return try {
            val list = json.decodeFromString<List<Drone>>(savedJson)
            if (list.isEmpty()) {
                Log.i("DroneRepository", "Список дронов пуст (все аппараты списаны).")
            }
            list
        } catch (e: Exception) {
            Log.e("DroneRepository", "Ошибка загрузки сохраненных дронов", e)
            startDrones()
        }
    }

    private fun loadSavedWeatherCache() {
        if (prefs == null || !prefs.contains("saved_weather_cache")) return
        val savedJson = prefs.getString("saved_weather_cache", null) ?: return
        try {
            val cacheMap = json.decodeFromString<Map<String, CachedWeather>>(savedJson)
            cityWeatherCache.putAll(cacheMap)
        } catch (e: Exception) {
            Log.e("DroneRepository", "Ошибка загрузки кэша погоды", e)
        }
    }

    private fun persistState() {
        if (prefs == null) return
        val encodedDrones = json.encodeToString(drones)
        val encodedCache = json.encodeToString(cityWeatherCache.toMap())
        val currentCredits = credits
        scope.launch(Dispatchers.IO) {
            try {
                prefs.edit()
                    .putString("saved_drones_list", encodedDrones)
                    .putString("saved_weather_cache", encodedCache)
                    .putInt("saved_credits", currentCredits)
                    .apply()
            } catch (e: Exception) {
                Log.e("DroneRepository", "Ошибка сохранения состояния", e)
            }
        }
    }

    private fun refreshWeatherForActiveMissions() {
        val activeCities = drones.filter { it.activeMissionRemaining > 0 }.map { it.baseCity }.distinct()
        if (activeCities.isNotEmpty()) {
            scope.launch {
                activeCities.forEach { cityName ->
                    try {
                        getOrFetchWeather(cityName, forceNetwork = false)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("DroneRepository", "Ошибка обновления погоды для активных миссий в городе $cityName", e)
                    }
                }
            }
        }
    }

    private suspend fun getOrFetchWeather(cityName: String, forceNetwork: Boolean): CurrentWeatherDto {
        val base = baseFor(cityName)
        val cached = cityWeatherCache[base.city]

        if (!forceNetwork && cached != null && cached.isFresh) {
            return cached.weather
        }

        // Один запрос на город: новые вызовы ждут уже идущий.
        val request = inFlight[base.city]
            ?: WeatherRequest(scope.async(start = CoroutineStart.LAZY) { loadFromNetwork(base) })
                .also { inFlight[base.city] = it }

        request.waiters++
        try {
            return request.deferred.await()
        } finally {
            request.waiters--
            // Ушли все ожидающие (например, пользователь нажал «Назад») — запрос отменяется.
            if (request.waiters == 0) {
                if (inFlight[base.city] === request) inFlight.remove(base.city)
                request.deferred.cancel()
            }
        }
    }

    private suspend fun loadFromNetwork(base: DroneBase): CurrentWeatherDto {
        val response = try {
            retryIO { openMeteoApi.getWeather(base.latitude, base.longitude) }
        } catch (e: IOException) {
            serverReachable = false
            throw e
        } catch (e: HttpException) {
            serverReachable = true
            throw e
        } catch (e: SerializationException) {
            serverReachable = true
            throw e
        }
        serverReachable = true
        val dto = response.currentWeather
        cityWeatherCache[base.city] = CachedWeather(dto, timestamp = System.currentTimeMillis())
        persistState()
        return dto
    }

    private fun isStormNow(city: String): Boolean =
        cityWeatherCache[city]?.takeIf { !it.isExpired }?.weather?.toDomain()?.isStormy == true

    private fun buildCachedTelemetry(
        target: Drone,
        base: DroneBase,
        cached: CachedWeather,
        notePrefix: String,
        canRetry: Boolean = true,
    ): DroneTelemetry {
        val expiredStr = if (cached.isExpired) " [УСТАРЕЛО > 2ч]" else ""
        return DroneTelemetry(
            model = "CyberScout ${target.name}",
            firmware = "v3.12.8-cyber-OFFLINE",
            rangeKm = 100 + (target.id * 10),
            sensorType = "LiDAR (из кэша)",
            weather = cached.weather.toDomain(),
            notes = "$notePrefix Кэш (${base.city}, возраст: ${(System.currentTimeMillis() - cached.timestamp) / 1000}с)$expiredStr.",
            isOnlineData = false,
            canRetry = canRetry,
            isExpired = cached.isExpired,
        )
    }

    suspend fun fetchTelemetry(id: Int, forceRefresh: Boolean = false): DroneTelemetry {
        val target = drone(id) ?: throw DroneNotFoundException()
        val base = baseFor(target.baseCity)

        // Без сети свежий кэш нельзя показывать как «Онлайн».
        val fresh = cityWeatherCache[base.city]?.takeIf { it.isFresh }
        if (!forceRefresh && !isOnline && fresh != null) {
            return buildCachedTelemetry(target, base, fresh, "Нет сети.")
        }

        try {
            val dto = getOrFetchWeather(base.city, forceNetwork = forceRefresh)
            return DroneTelemetry(
                model = "CyberScout ${target.name}",
                firmware = "v3.12.8-cyber-LIVE",
                rangeKm = 100 + (target.id * 10),
                sensorType = "LiDAR / Quantum Optical (Онлайн)",
                weather = dto.toDomain(),
                notes = "Прямой поток метеоданных успешно получен по сети с Open-Meteo API (${base.city}).",
                isOnlineData = true,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            val cached = cityWeatherCache[base.city]
            if (cached != null) {
                return buildCachedTelemetry(target, base, cached, "Нет сети.")
            }
            throw DroneNetworkException(
                reason = NetworkErrorReason.NO_NETWORK,
                canRetry = true,
                cause = e,
            )
        } catch (e: HttpException) {
            val cached = cityWeatherCache[base.city]
            val canRetry = e.code() == 408 || e.code() == 429 || e.code() >= 500
            val reason = when (e.code()) {
                429 -> NetworkErrorReason.RATE_LIMIT
                in 500..599 -> NetworkErrorReason.SERVER_ERROR
                else -> NetworkErrorReason.OTHER
            }
            if (cached != null) {
                return buildCachedTelemetry(target, base, cached, "Ошибка сервера (${e.code()}).", canRetry)
            }
            throw DroneNetworkException(
                reason = reason,
                canRetry = canRetry,
                cause = e,
            )
        } catch (e: SerializationException) {
            Log.e("DroneRepository", "Ошибка десериализации ответа сервера для дрона $id", e)
            val cached = cityWeatherCache[base.city]
            if (cached != null) {
                return buildCachedTelemetry(target, base, cached, "Ошибка формата.", canRetry = false)
            }
            throw DroneNetworkException(
                reason = NetworkErrorReason.FORMAT_ERROR,
                canRetry = false,
                cause = e,
            )
        } catch (e: Exception) {
            Log.e("DroneRepository", "Неожиданная ошибка при получении телеметрии для дрона $id", e)
            val cached = cityWeatherCache[base.city]
            if (cached != null) {
                return buildCachedTelemetry(target, base, cached, "Системная ошибка.")
            }
            throw DroneNetworkException(
                reason = NetworkErrorReason.OTHER,
                canRetry = true,
                cause = e,
            )
        }
    }

    fun addDrone(customName: String? = null) {
        val defaultName = "Дрон X-%02d".format(nextId)
        val droneName = if (customName.isNullOrBlank()) defaultName else customName.trim()
        val defaultBase = ALL_BASES[(nextId - 1) % ALL_BASES.size].city
        val newDrone = Drone(
            id = nextId,
            name = droneName,
            baseCity = defaultBase,
            battery = 100,
            cargo = 0,
            temperature = 20,
        )
        nextId++
        drones = drones + newDrone
        persistState()
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        lastDeletedDrone = target
        drones = drones - target
        persistState()

        undoTimerSeconds = 10
        undoJob?.cancel()
        undoJob = scope.launch {
            while (undoTimerSeconds > 0) {
                delay(1.seconds)
                undoTimerSeconds = undoTimerSeconds - 1
            }
            lastDeletedDrone = null
        }
    }

    fun restoreLastDeletedDrone() {
        undoJob?.cancel()
        val target = lastDeletedDrone
        if ((target != null) && (drone(target.id) == null)) {
            drones = drones + target
        }
        lastDeletedDrone = null
        undoTimerSeconds = 0
        persistState()
    }

    fun clearMissionNotice() {
        missionNoticeJob?.cancel()
        unhandledCompletedMissions.clear()
        totalPendingReward = 0
        missionMessage = null
        missionNoticeTimerSeconds = 0
    }

    private fun triggerMissionNotice(droneName: String, reward: Int, cargoGain: Int) {
        unhandledCompletedMissions.add(droneName)
        totalPendingReward += reward

        val count = unhandledCompletedMissions.size
        missionMessage = DroneGameEngine.formatMissionNotice(
            droneName = droneName,
            reward = reward,
            cargoGain = cargoGain,
            queueCount = count,
            totalReward = totalPendingReward,
        )

        missionNoticeTimerSeconds = 8
        missionNoticeJob?.cancel()
        missionNoticeJob = scope.launch {
            while (missionNoticeTimerSeconds > 0) {
                delay(1.seconds)
                missionNoticeTimerSeconds = missionNoticeTimerSeconds - 1
            }
            clearMissionNotice()
        }
    }

    private fun triggerStormNotice(droneName: String) {
        missionMessage = DroneGameEngine.formatStormNotice(droneName)
        missionNoticeTimerSeconds = 8
        missionNoticeJob?.cancel()
        missionNoticeJob = scope.launch {
            while (missionNoticeTimerSeconds > 0) {
                delay(1.seconds)
                missionNoticeTimerSeconds = missionNoticeTimerSeconds - 1
            }
            clearMissionNotice()
        }
    }

    fun renameDrone(id: Int, newName: String) {
        val current = drone(id) ?: return
        if (newName.isBlank()) return
        val updated = current.copy(name = newName.trim())
        updateDrone(updated)
    }

    fun setDroneBase(id: Int, baseCity: String) {
        val current = drone(id) ?: return
        val updated = current.copy(baseCity = baseCity)
        updateDrone(updated)
    }

    fun collectData(id: Int, isWindy: Boolean = false) {
        val current = drone(id) ?: return
        if (!current.canCollectData) return
        val cost = if (isWindy) WINDY_COLLECT_COST else NORMAL_COLLECT_COST
        val updated = current.copy(
            cargo = (current.cargo + 25).coerceAtMost(current.maxCargo),
            battery = (current.battery - cost).coerceAtLeast(0),
            temperature = (current.temperature + current.heatRate).coerceAtMost(100),
        )
        updateDrone(updated)
    }

    fun transmitData(id: Int) {
        val current = drone(id) ?: return
        if (!current.canTransmitData) return
        val reward = current.cargo / 2
        credits += reward
        val updated = current.copy(
            cargo = 0,
            battery = (current.battery - 15).coerceAtLeast(0),
            temperature = (current.temperature - 20).coerceAtLeast(20),
        )
        updateDrone(updated)
    }

    fun hibernate(id: Int) {
        val current = drone(id) ?: return
        if (!current.canHibernate) return
        val updated = current.copy(
            battery = current.maxBattery,
            temperature = 20,
        )
        updateDrone(updated)
    }

    fun resetDrone(id: Int) {
        val current = drone(id) ?: return
        val updated = current.copy(
            rankLevel = 1,
            battery = 100,
            maxBattery = 100,
            cargo = 0,
            maxCargo = 100,
            temperature = 20,
            heatRate = 15,
            missionsCompleted = 0,
            activeMissionDuration = 0,
            activeMissionRemaining = 0,
            missionIsStormy = false,
        )
        updateDrone(updated)
    }

    fun hibernateAll() {
        drones = drones.map { drone ->
            if (drone.canHibernate) {
                drone.copy(battery = drone.maxBattery, temperature = 20)
            } else drone
        }
        persistState()
    }

    fun transmitAll() {
        var totalReward = 0
        drones = drones.map { drone ->
            if (drone.canTransmitData) {
                totalReward += drone.cargo / 2
                drone.copy(
                    cargo = 0,
                    battery = (drone.battery - 15).coerceAtLeast(0),
                    temperature = (drone.temperature - 20).coerceAtLeast(20),
                )
            } else drone
        }
        credits += totalReward
        persistState()
    }

    fun startMission(id: Int, durationSeconds: Int) {
        val current = drone(id) ?: return
        val launchCost = current.missionLaunchCost(durationSeconds)
        if (!current.canStartMission(durationSeconds, credits)) return
        credits -= launchCost

        val isLaunchStormy = isStormNow(current.baseCity)

        scope.launch {
            try {
                getOrFetchWeather(current.baseCity, forceNetwork = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DroneRepository", "Ошибка подгрузки погоды перед миссией дрона $id", e)
            }
        }

        val batteryCost = DroneGameEngine.batteryCostForMission(durationSeconds)
        val updated = current.copy(
            activeMissionDuration = durationSeconds,
            activeMissionRemaining = durationSeconds,
            missionIsStormy = isLaunchStormy,
            battery = (current.battery - batteryCost).coerceAtLeast(0),
        )
        updateDrone(updated)
    }

    private fun tickMissions() {
        var stateChanged = false
        var forcePersist = false

        val updatedDrones = drones.map { drone ->
            if (drone.activeMissionRemaining > 0) {
                stateChanged = true
                val newRemaining = drone.activeMissionRemaining - 1
                val isCityStormy = drone.missionIsStormy || isStormNow(drone.baseCity)

                if (isCityStormy && (drone.activeMissionDuration >= 30) && (newRemaining == (drone.activeMissionDuration / 2))) {
                    forcePersist = true
                    triggerStormNotice(drone.name)
                    drone.copy(
                        activeMissionDuration = 0,
                        activeMissionRemaining = 0,
                        missionIsStormy = false,
                        cargo = 0,
                        battery = 0,
                        temperature = 90,
                    )
                } else if (newRemaining <= 0) {
                    forcePersist = true
                    val reward = DroneGameEngine.missionReward(drone.activeMissionDuration)
                    val cargoGain = DroneGameEngine.cargoGain(drone.activeMissionDuration)
                    val expGain = DroneGameEngine.expGain(drone.activeMissionDuration)
                    credits += reward
                    val (totalMissions, newRankLevel) = DroneGameEngine.calculateRank(drone.missionsCompleted, expGain)
                    triggerMissionNotice(drone.name, reward, cargoGain)
                    drone.copy(
                        activeMissionDuration = 0,
                        activeMissionRemaining = 0,
                        missionIsStormy = false,
                        missionsCompleted = totalMissions,
                        rankLevel = newRankLevel,
                        cargo = (drone.cargo + cargoGain).coerceAtMost(drone.maxCargo),
                        temperature = (drone.temperature + 10).coerceAtMost(100),
                    )
                } else {
                    drone.copy(activeMissionRemaining = newRemaining)
                }
            } else drone
        }
        if (stateChanged) {
            drones = updatedDrones
            val periodicSave = updatedDrones.any { it.activeMissionRemaining > 0 && it.activeMissionRemaining % 5 == 0 }
            if (forcePersist || periodicSave) {
                persistState()
            }
        }
    }

    fun upgradeBattery(id: Int): Boolean {
        val current = drone(id) ?: return false
        val cost = current.upgradeCost
        if (credits < cost) return false
        credits -= cost
        val newMax = current.maxBattery + current.batteryBoostAmount
        val updated = current.copy(
            maxBattery = newMax,
            battery = (current.battery + current.batteryBoostAmount).coerceAtMost(newMax),
        )
        updateDrone(updated)
        return true
    }

    fun upgradeCargo(id: Int): Boolean {
        val current = drone(id) ?: return false
        val cost = current.upgradeCost
        if (credits < cost) return false
        credits -= cost
        val newMax = current.maxCargo + current.cargoBoostAmount
        val updated = current.copy(
            maxCargo = newMax,
            cargo = current.cargo.coerceAtMost(newMax),
        )
        updateDrone(updated)
        return true
    }

    fun upgradeCooling(id: Int): Boolean {
        val current = drone(id) ?: return false
        val cost = current.upgradeCost
        if (credits < cost) return false
        if (current.heatRate <= 5) return false
        credits -= cost
        val updated = current.copy(heatRate = (current.heatRate - current.coolingBoostAmount).coerceAtLeast(5))
        updateDrone(updated)
        return true
    }

    private fun updateDrone(updated: Drone) {
        drones = drones.map { if (it.id == updated.id) updated else it }
        persistState()
    }
}
