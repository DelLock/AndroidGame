package com.example.androidgame.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.androidgame.data.api.OpenMeteoApi
import com.example.androidgame.data.api.dto.toDomain
import com.example.androidgame.domain.model.ALL_BASES
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.DroneGameEngine
import com.example.androidgame.domain.model.DroneTelemetry
import com.example.androidgame.domain.model.NORMAL_COLLECT_COST
import com.example.androidgame.domain.model.WINDY_COLLECT_COST
import com.example.androidgame.domain.model.baseFor
import com.example.androidgame.domain.model.startDrones
import com.example.androidgame.util.retryIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val context: Context? = null,
    mainContext: CoroutineContext = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(mainContext + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences? = context?.getSharedPreferences("drone_hub_storage", Context.MODE_PRIVATE)

    private val _credits = MutableStateFlow(prefs?.getInt("saved_credits", 300) ?: 300)
    val creditsFlow: StateFlow<Int> = _credits.asStateFlow()
    val credits: Int get() = _credits.value

    private val _drones = MutableStateFlow(loadSavedDrones())
    val dronesFlow: StateFlow<List<Drone>> = _drones.asStateFlow()
    val drones: List<Drone> get() = _drones.value

    private val _lastDeletedDrone = MutableStateFlow<Drone?>(null)
    val lastDeletedDroneFlow: StateFlow<Drone?> = _lastDeletedDrone.asStateFlow()
    val lastDeletedDrone: Drone? get() = _lastDeletedDrone.value

    private val _undoTimerSeconds = MutableStateFlow(0)
    val undoTimerSecondsFlow: StateFlow<Int> = _undoTimerSeconds.asStateFlow()
    val undoTimerSeconds: Int get() = _undoTimerSeconds.value

    private val _lastCompletedMissionMessage = MutableStateFlow<String?>(null)
    val lastCompletedMissionMessageFlow: StateFlow<String?> = _lastCompletedMissionMessage.asStateFlow()
    val lastCompletedMissionMessage: String? get() = _lastCompletedMissionMessage.value

    private val _missionNoticeTimerSeconds = MutableStateFlow(0)
    val missionNoticeTimerSecondsFlow: StateFlow<Int> = _missionNoticeTimerSeconds.asStateFlow()
    val missionNoticeTimerSeconds: Int get() = _missionNoticeTimerSeconds.value

    private val cityWeatherCache = mutableMapOf<String, CachedWeather>()
    private val unhandledCompletedMissions = mutableListOf<String>()
    private var totalPendingReward = 0

    private var undoJob: Job? = null
    private var missionNoticeJob: Job? = null
    private var nextId = (drones.maxOfOrNull { it.id } ?: 0) + 1

    init {
        loadSavedWeatherCache()
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
        try {
            val encodedDrones = json.encodeToString(drones)
            val encodedCache = json.encodeToString(cityWeatherCache.toMap())
            prefs.edit()
                .putString("saved_drones_list", encodedDrones)
                .putString("saved_weather_cache", encodedCache)
                .putInt("saved_credits", credits)
                .apply()
        } catch (e: Exception) {
            Log.e("DroneRepository", "Ошибка сохранения состояния", e)
        }
    }

    suspend fun fetchTelemetry(id: Int): DroneTelemetry {
        val target = drone(id) ?: throw DroneNotFoundException()
        val base = baseFor(target.baseCity)

        try {
            val response = retryIO { openMeteoApi.getWeather(base.latitude, base.longitude) }
            cityWeatherCache[base.city] = CachedWeather(response.currentWeather, timestamp = System.currentTimeMillis())
            persistState()
            return DroneTelemetry(
                model = "CyberScout ${target.name}",
                firmware = "v3.12.8-cyber-LIVE",
                rangeKm = 100 + (target.id * 10),
                sensorType = "LiDAR / Quantum Optical (Онлайн)",
                weather = response.currentWeather.toDomain(),
                notes = "Прямой поток метеоданных успешно получен по сети с Open-Meteo API (${base.city}).",
                isOnlineData = true,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            val cached = cityWeatherCache[base.city]
            if (cached != null) {
                val expiredStr = if (cached.isExpired) " [УСТАРЕЛО > 2ч]" else ""
                return DroneTelemetry(
                    model = "CyberScout ${target.name}",
                    firmware = "v3.12.8-cyber-OFFLINE",
                    rangeKm = 100 + (target.id * 10),
                    sensorType = "LiDAR (из кэша)",
                    weather = cached.weather.toDomain(),
                    notes = "Сеть недоступна. Кэш (${base.city}, возраст: ${(System.currentTimeMillis() - cached.timestamp) / 1000}с)$expiredStr.",
                    isOnlineData = false,
                )
            }
            throw DroneNetworkException(canRetry = true)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                throw DroneNotFoundException()
            } else if (e.code() >= 500) {
                val cached = cityWeatherCache[base.city]
                if (cached != null) {
                    return DroneTelemetry(
                        model = "CyberScout ${target.name}",
                        firmware = "v3.12.8-cyber-OFFLINE",
                        rangeKm = 100 + (target.id * 10),
                        sensorType = "LiDAR (из кэша)",
                        weather = cached.weather.toDomain(),
                        notes = "Ошибка сервера. Показаны кэшированные метеоданные.",
                        isOnlineData = false,
                    )
                }
                throw DroneNetworkException(canRetry = true, cause = e)
            } else {
                throw DroneNetworkException(canRetry = false, cause = e)
            }
        } catch (e: SerializationException) {
            Log.e("DroneRepository", "Ошибка десериализации ответа сервера для дрона $id", e)
            val cached = cityWeatherCache[base.city]
            if (cached != null) {
                return DroneTelemetry(
                    model = "CyberScout ${target.name}",
                    firmware = "v3.12.8-cyber-OFFLINE",
                    rangeKm = 100 + (target.id * 10),
                    sensorType = "LiDAR (из кэша)",
                    weather = cached.weather.toDomain(),
                    notes = "Ошибка десериализации. Показаны кэшированные метеоданные.",
                    isOnlineData = false,
                )
            }
            throw DroneNetworkException(canRetry = false, cause = e)
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
        _drones.value = _drones.value + newDrone
        persistState()
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        _lastDeletedDrone.value = target
        _drones.value = _drones.value - target
        persistState()

        _undoTimerSeconds.value = 10
        undoJob?.cancel()
        undoJob = scope.launch {
            while (_undoTimerSeconds.value > 0) {
                delay(1.seconds)
                _undoTimerSeconds.value = _undoTimerSeconds.value - 1
            }
            _lastDeletedDrone.value = null
        }
    }

    fun restoreLastDeletedDrone() {
        undoJob?.cancel()
        val target = lastDeletedDrone
        if ((target != null) && (drone(target.id) == null)) {
            _drones.value = _drones.value + target
        }
        _lastDeletedDrone.value = null
        _undoTimerSeconds.value = 0
        persistState()
    }

    fun clearMissionNotice() {
        missionNoticeJob?.cancel()
        unhandledCompletedMissions.clear()
        totalPendingReward = 0
        _lastCompletedMissionMessage.value = null
        _missionNoticeTimerSeconds.value = 0
    }

    private fun triggerMissionNotice(droneName: String, reward: Int, cargoGain: Int) {
        unhandledCompletedMissions.add(droneName)
        totalPendingReward += reward

        val count = unhandledCompletedMissions.size
        _lastCompletedMissionMessage.value = if (count == 1) {
            "Дрон '$droneName' успешно вернулся с вылета! (+$reward⚡, +$cargoGain% памяти)"
        } else {
            "Дрон '$droneName' вернулся! (+$reward⚡, еще +${count - 1} в очереди, всего +$totalPendingReward⚡)"
        }

        _missionNoticeTimerSeconds.value = 8
        missionNoticeJob?.cancel()
        missionNoticeJob = scope.launch {
            while (_missionNoticeTimerSeconds.value > 0) {
                delay(1.seconds)
                _missionNoticeTimerSeconds.value = _missionNoticeTimerSeconds.value - 1
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
        _credits.value += reward
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
        )
        updateDrone(updated)
    }

    fun hibernateAll() {
        _drones.value = _drones.value.map { drone ->
            if (drone.canHibernate) {
                drone.copy(battery = drone.maxBattery, temperature = 20)
            } else drone
        }
        persistState()
    }

    fun transmitAll() {
        var totalReward = 0
        _drones.value = _drones.value.map { drone ->
            if (drone.canTransmitData) {
                totalReward += drone.cargo / 2
                drone.copy(
                    cargo = 0,
                    battery = (drone.battery - 15).coerceAtLeast(0),
                    temperature = (drone.temperature - 20).coerceAtLeast(20),
                )
            } else drone
        }
        _credits.value += totalReward
        persistState()
    }

    fun startMission(id: Int, durationSeconds: Int) {
        val current = drone(id) ?: return
        val launchCost = current.missionLaunchCost(durationSeconds)
        if (!current.canStartMission(durationSeconds, credits)) return
        _credits.value -= launchCost

        val cached = cityWeatherCache[current.baseCity]
        if (cached == null || cached.isExpired) {
            scope.launch {
                try {
                    val base = baseFor(current.baseCity)
                    val response = openMeteoApi.getWeather(base.latitude, base.longitude)
                    cityWeatherCache[base.city] = CachedWeather(response.currentWeather, timestamp = System.currentTimeMillis())
                    persistState()
                } catch (_: Exception) {}
            }
        }

        val batteryCost = DroneGameEngine.batteryCostForMission(durationSeconds)
        val updated = current.copy(
            activeMissionDuration = durationSeconds,
            activeMissionRemaining = durationSeconds,
            battery = (current.battery - batteryCost).coerceAtLeast(0),
        )
        updateDrone(updated)
    }

    private fun tickMissions() {
        var stateChanged = false
        val updatedDrones = _drones.value.map { drone ->
            if (drone.activeMissionRemaining > 0) {
                stateChanged = true
                val newRemaining = drone.activeMissionRemaining - 1
                val cachedWeather = cityWeatherCache[drone.baseCity]
                val isCityStormy = if (cachedWeather != null && !cachedWeather.isExpired) {
                    val domainWeather = cachedWeather.weather.toDomain()
                    domainWeather.isStormy
                } else false

                if (isCityStormy && (drone.activeMissionDuration >= 30) && (newRemaining == (drone.activeMissionDuration / 2))) {
                    triggerMissionNotice(drone.name, 0, 0)
                    drone.copy(
                        activeMissionDuration = 0,
                        activeMissionRemaining = 0,
                        cargo = 0,
                        battery = 0,
                        temperature = 90,
                    )
                } else if (newRemaining <= 0) {
                    val reward = DroneGameEngine.missionReward(drone.activeMissionDuration)
                    val cargoGain = DroneGameEngine.cargoGain(drone.activeMissionDuration)
                    val expGain = DroneGameEngine.expGain(drone.activeMissionDuration)
                    _credits.value += reward
                    val (totalMissions, newRankLevel) = DroneGameEngine.calculateRank(drone.missionsCompleted, expGain)
                    triggerMissionNotice(drone.name, reward, cargoGain)
                    drone.copy(
                        activeMissionDuration = 0,
                        activeMissionRemaining = 0,
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
            _drones.value = updatedDrones
            persistState()
        }
    }

    fun upgradeBattery(id: Int): Boolean {
        val current = drone(id) ?: return false
        val cost = current.upgradeCost
        if (credits < cost) return false
        _credits.value -= cost
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
        _credits.value -= cost
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
        _credits.value -= cost
        val updated = current.copy(heatRate = (current.heatRate - current.coolingBoostAmount).coerceAtLeast(5))
        updateDrone(updated)
        return true
    }

    private fun updateDrone(updated: Drone) {
        _drones.value = _drones.value.map { if (it.id == updated.id) updated else it }
        persistState()
    }
}
