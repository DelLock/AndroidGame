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

    var drones by mutableStateOf(loadSavedDrones())
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

    private fun persistDrones(list: List<Drone>) {
        if (prefs == null) return
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString("saved_drones_list", encoded).apply()
        } catch (e: Exception) {
            Log.e("DroneRepository", "Ошибка сохранения списка дронов", e)
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
                // Выводим локальную телеметрию из assets при отсутствии сети
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
        val newDrone = Drone(
            id = nextId,
            name = droneName,
            battery = 100,
            cargo = 0,
            temperature = 20,
        )
        nextId++
        drones += newDrone
        persistDrones(drones)
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        drones -= target
        persistDrones(drones)
    }

    fun renameDrone(id: Int, newName: String) {
        val current = drone(id) ?: return
        if (newName.isBlank()) return
        val updated = current.copy(name = newName.trim())
        updateDrone(updated)
    }

    fun collectData(id: Int, isWindy: Boolean = false) {
        val current = drone(id) ?: return
        if (!current.canCollectData) return
        val cost = if (isWindy) WINDY_COLLECT_COST else NORMAL_COLLECT_COST
        val updated = current.copy(
            cargo = (current.cargo + 25).coerceAtMost(100),
            battery = (current.battery - cost).coerceAtLeast(0),
            temperature = (current.temperature + 15).coerceAtMost(100),
        )
        updateDrone(updated)
    }

    fun transmitData(id: Int) {
        val current = drone(id) ?: return
        if (!current.canTransmitData) return
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
            battery = 100,
            temperature = 20,
        )
        updateDrone(updated)
    }

    fun resetDrone(id: Int) {
        val current = drone(id) ?: return
        val updated = current.copy(
            battery = 100,
            cargo = 0,
            temperature = 20,
        )
        updateDrone(updated)
    }

    private fun updateDrone(updated: Drone) {
        drones = drones.map { if (it.id == updated.id) updated else it }
        persistDrones(drones)
    }
}
