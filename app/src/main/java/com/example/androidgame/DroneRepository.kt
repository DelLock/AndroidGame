package com.example.androidgame

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class DroneRepository(
    private val context: Context? = null,
    private val api: DroneApi? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences? = context?.getSharedPreferences("drone_hub_storage", Context.MODE_PRIVATE)

    var credits by mutableStateOf(prefs?.getInt("saved_credits", 300) ?: 300)
        private set

    var drones by mutableStateOf(loadSavedDrones())
        private set

    var lastDeletedDrone by mutableStateOf<Drone?>(null)
        private set

    private var nextId = (drones.maxOfOrNull { it.id } ?: 0) + 1

    fun drone(id: Int): Drone? = drones.find { it.id == id }

    private fun loadSavedDrones(): List<Drone> {
        val savedJson = prefs?.getString("saved_drones_list", null)
        if (!savedJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<Drone>>(savedJson)
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.e("DroneRepository", "Ошибка загрузки сохраненных дронов", e)
            }
        }
        return startDrones()
    }

    private fun persistState() {
        if (prefs == null) return
        try {
            val encoded = json.encodeToString(drones)
            prefs.edit()
                .putString("saved_drones_list", encoded)
                .putInt("saved_credits", credits)
                .apply()
        } catch (e: Exception) {
            Log.e("DroneRepository", "Ошибка сохранения состояния", e)
        }
    }

    suspend fun fetchTelemetry(id: Int): DroneTelemetryDto {
        val target = drone(id) ?: throw DroneNotFoundException()

        if (api != null) {
            try {
                return retryIO { api.getTelemetry(id) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                // Если нет сети, выводим локальную телеметрию из assets
            } catch (e: HttpException) {
                if (e.code() == 404) throw DroneNotFoundException()
                throw DroneNetworkException(canRetry = e.code() >= 500, cause = e)
            } catch (e: SerializationException) {
                Log.e("DroneRepository", "Ошибка десериализации ответа сервера для дрона $id", e)
                throw DroneNetworkException(canRetry = false, cause = e)
            }
        }

        if (context != null) {
            return withContext(Dispatchers.IO) {
                try {
                    val assetPath = "api-data/telemetry-$id.json"
                    context.assets.open(assetPath).use { stream ->
                        val jsonString = stream.bufferedReader().readText()
                        json.decodeFromString<DroneTelemetryDto>(jsonString)
                    }
                } catch (_: IOException) {
                    DroneTelemetryDto(
                        model = "CyberScout ${target.name}",
                        firmware = "v3.12.8-cyber",
                        rangeKm = 100 + (target.id * 10),
                        sensorType = "LiDAR / Thermal Array",
                        notes = "Спецификация телеметрии штаба для дрона №$id.",
                    )
                } catch (e: SerializationException) {
                    throw DroneNetworkException(canRetry = false, cause = e)
                }
            }
        }

        return DroneTelemetryDto(
            model = "CyberScout ${target.name}",
            firmware = "v3.12.8-cyber",
            rangeKm = 100 + (target.id * 10),
            sensorType = "LiDAR / Thermal Array",
            notes = "Спецификация телеметрии штаба для дрона №$id.",
        )
    }

    fun addDrone(customName: String? = null) {
        val defaultName = "Дрон-разведчик X-%02d".format(nextId)
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
        drones += newDrone
        persistState()
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        lastDeletedDrone = target
        drones -= target
        persistState()
    }

    fun restoreLastDeletedDrone() {
        val target = lastDeletedDrone ?: return
        if (drone(target.id) == null) {
            drones += target
            lastDeletedDrone = null
            persistState()
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
            cargo = (current.cargo + 25).coerceAtMost(100),
            battery = (current.battery - cost).coerceAtLeast(0),
            temperature = (current.temperature + current.heatRate).coerceAtMost(100),
        )
        updateDrone(updated)
    }

    fun transmitData(id: Int) {
        val current = drone(id) ?: return
        if (!current.canTransmitData) return
        val reward = current.cargo * 2 * current.rankLevel
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
            battery = current.maxBattery,
            cargo = 0,
            temperature = 20,
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
                totalReward += drone.cargo * 2 * drone.rankLevel
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

    fun tickMissions(isStormyWeather: Boolean = false) {
        var stateChanged = false
        drones = drones.map { drone ->
            if (drone.activeMissionRemaining > 0) {
                stateChanged = true
                val newRemaining = drone.activeMissionRemaining - 1

                if (isStormyWeather && (drone.activeMissionDuration >= 30) && (newRemaining == (drone.activeMissionDuration / 2))) {
                    drone.copy(
                        activeMissionDuration = 0,
                        activeMissionRemaining = 0,
                        cargo = 0,
                        battery = 0,
                        temperature = 90,
                    )
                } else if (newRemaining <= 0) {
                    val reward = when (drone.activeMissionDuration) {
                        60 -> 900 * drone.rankLevel
                        30 -> 300 * drone.rankLevel
                        else -> 100 * drone.rankLevel
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
                    drone.copy(
                        activeMissionDuration = 0,
                        activeMissionRemaining = 0,
                        missionsCompleted = totalMissions,
                        rankLevel = newRankLevel,
                        cargo = (drone.cargo + cargoGain).coerceAtMost(100),
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
