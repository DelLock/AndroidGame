package com.example.androidgame.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.androidgame.data.api.OpenMeteoApi
import com.example.androidgame.data.api.dto.CurrentWeatherDto
import com.example.androidgame.data.api.dto.DroneNetworkException
import com.example.androidgame.data.api.dto.DroneNotFoundException
import com.example.androidgame.data.api.dto.DroneTelemetryDto
import com.example.androidgame.data.api.dto.NORMAL_COLLECT_COST
import com.example.androidgame.data.api.dto.WINDY_COLLECT_COST
import com.example.androidgame.data.api.dto.baseFor
import com.example.androidgame.data.api.dto.isStormy
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.startDrones
import com.example.androidgame.util.retryIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private val context: Context? = null,
    mainContext: CoroutineContext = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(mainContext + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences? = context?.getSharedPreferences("drone_hub_storage", Context.MODE_PRIVATE)

    var credits by mutableStateOf(prefs?.getInt("saved_credits", 300) ?: 300)
        private set

    var drones by mutableStateOf(loadSavedDrones())
        private set

    var lastDeletedDrone by mutableStateOf<Drone?>(null)
        private set

    var undoTimerSeconds by mutableStateOf(0)
        private set

    var lastCompletedMissionMessage by mutableStateOf<String?>(null)
        private set

    var missionNoticeTimerSeconds by mutableStateOf(0)
        private set

    private val cityWeatherCache = mutableStateMapOf<String, CurrentWeatherDto>()
    private val unhandledCompletedMissions = mutableListOf<String>()
    private var totalPendingReward = 0

    private var undoJob: Job? = null
    private var missionNoticeJob: Job? = null
    private var nextId = (drones.maxOfOrNull { it.id } ?: 0) + 1
    private var lastTickTimestamp = 0L

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
            val cacheMap = json.decodeFromString<Map<String, CurrentWeatherDto>>(savedJson)
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

    suspend fun fetchTelemetry(id: Int): DroneTelemetryDto {
        val target = drone(id) ?: throw DroneNotFoundException()
        val base = baseFor(target.baseCity)

        try {
            val response = retryIO { openMeteoApi.getWeather(base.latitude, base.longitude) }
            cityWeatherCache[base.city] = response.currentWeather
            return DroneTelemetryDto(
                model = "CyberScout ${target.name}",
                firmware = "v3.12.8-cyber-LIVE",
                rangeKm = 100 + (target.id * 10),
                sensorType = "LiDAR / Quantum Optical (Онлайн)",
                weather = response.currentWeather,
                notes = "Прямой поток метеоданных успешно получен по сети с Open-Meteo API (${base.city}).",
                isOnlineData = true,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            throw DroneNetworkException(canRetry = true)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                throw DroneNotFoundException()
            } else if (e.code() >= 500) {
                throw DroneNetworkException(canRetry = true, cause = e)
            } else {
                throw DroneNetworkException(canRetry = false, cause = e)
            }
        } catch (e: SerializationException) {
            Log.e("DroneRepository", "Ошибка десериализации ответа сервера для дрона $id", e)
            throw DroneNetworkException(canRetry = false, cause = e)
        }
    }

    fun addDrone(customName: String? = null) {
        val defaultName = "Дрон X-%02d".format(nextId)
        val droneName = if (customName.isNullOrBlank()) defaultName else customName.trim()
        val defaultBase = com.example.androidgame.data.api.dto.ALL_BASES[(nextId - 1) % com.example.androidgame.data.api.dto.ALL_BASES.size].city
        val newDrone = Drone(
            id = nextId,
            name = droneName,
            baseCity = defaultBase,
            battery = 100,
            cargo = 0,
            temperature = 20,
        )
        nextId++
        drones += newDrone
        persistState()
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        lastDeletedDrone = target
        drones -= target
        persistState()

        undoTimerSeconds = 10
        undoJob?.cancel()
        undoJob = scope.launch {
            while (undoTimerSeconds > 0) {
                delay(1.seconds)
                undoTimerSeconds--
            }
            lastDeletedDrone = null
        }
    }

    fun restoreLastDeletedDrone() {
        undoJob?.cancel()
        val target = lastDeletedDrone
        if ((target != null) && (drone(target.id) == null)) {
            drones += target
        }
        lastDeletedDrone = null
        undoTimerSeconds = 0
        persistState()
    }

    fun clearMissionNotice() {
        missionNoticeJob?.cancel()
        unhandledCompletedMissions.clear()
        totalPendingReward = 0
        lastCompletedMissionMessage = null
        missionNoticeTimerSeconds = 0
    }

    private fun triggerMissionNotice(droneName: String, reward: Int, cargoGain: Int) {
        unhandledCompletedMissions.add(droneName)
        totalPendingReward += reward

        val count = unhandledCompletedMissions.size
        lastCompletedMissionMessage = if (count == 1) {
            "Дрон '$droneName' успешно вернулся с вылета! (+$reward⚡, +$cargoGain% памяти)"
        } else {
            "Дрон '$droneName' вернулся! (+$reward⚡, еще +${count - 1} в очереди, всего +$totalPendingReward⚡)"
        }

        missionNoticeTimerSeconds = 8
        missionNoticeJob?.cancel()
        missionNoticeJob = scope.launch {
            while (missionNoticeTimerSeconds > 0) {
                delay(1.seconds)
                missionNoticeTimerSeconds--
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

        if (cityWeatherCache[current.baseCity] == null) {
            scope.launch {
                try {
                    val base = baseFor(current.baseCity)
                    val response = openMeteoApi.getWeather(base.latitude, base.longitude)
                    cityWeatherCache[base.city] = response.currentWeather
                    persistState()
                } catch (_: Exception) {}
            }
        }

        val batteryCost = when (durationSeconds) {
            60 -> 50
            30 -> 35
            else -> 20
        }
        val updated = current.copy(
            activeMissionDuration = durationSeconds,
            activeMissionRemaining = durationSeconds,
            battery = (current.battery - batteryCost).coerceAtLeast(0),
        )
        updateDrone(updated)
    }

    fun tickMissions() {
        val now = System.currentTimeMillis()
        if ((now - lastTickTimestamp) < 900L) return
        lastTickTimestamp = now

        var stateChanged = false
        drones = drones.map { drone ->
            if (drone.activeMissionRemaining > 0) {
                stateChanged = true
                val newRemaining = drone.activeMissionRemaining - 1
                val isCityStormy = cityWeatherCache[drone.baseCity]?.isStormy ?: false

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
                    val reward = when (drone.activeMissionDuration) {
                        60 -> 350
                        30 -> 150
                        else -> 60
                    }
                    val cargoGain = when (drone.activeMissionDuration) {
                        60 -> 100
                        30 -> 50
                        else -> 25
                    }
                    val expGain = when (drone.activeMissionDuration) {
                        60 -> 3
                        30 -> 2
                        else -> 1
                    }
                    credits += reward
                    val totalMissions = drone.missionsCompleted + expGain
                    val newRankLevel = ((totalMissions / 3) + 1).coerceAtMost(5)
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
        if (stateChanged) persistState()
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
