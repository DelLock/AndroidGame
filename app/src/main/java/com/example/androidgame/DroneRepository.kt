package com.example.androidgame

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class DroneRepository(private val api: DroneApi? = null) {
    var drones by mutableStateOf(startDrones())
        private set

    private var nextId = (drones.maxOfOrNull { it.id } ?: 0) + 1

    fun drone(id: Int): Drone? = drones.find { it.id == id }

    suspend fun fetchTelemetry(id: Int): DroneTelemetryDto {
        val target = drone(id) ?: throw DroneNotFoundException()
        if (api == null) {
            return DroneTelemetryDto(
                model = "CyberScout ${target.name}",
                firmware = "v3.12.8-cyber",
                rangeKm = 100 + target.id * 10,
                sensorType = "LiDAR / Thermal Array",
                notes = "Телеметрический канал штаба функционирует в норме.",
            )
        }
        return try {
            retryIO { api.getTelemetry(id) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw DroneNetworkException(canRetry = true, cause = e)
        } catch (e: HttpException) {
            if (e.code() == 404) throw DroneNotFoundException()
            throw DroneNetworkException(canRetry = e.code() >= 500, cause = e)
        } catch (e: SerializationException) {
            Log.e("DroneRepository", "Ошибка десериализации ответа сервера для дрона $id", e)
            throw DroneNetworkException(canRetry = false, cause = e)
        }
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
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        drones -= target
    }

    fun renameDrone(id: Int, newName: String) {
        val current = drone(id) ?: return
        if (newName.isBlank()) return
        val updated = current.copy(name = newName.trim())
        updateDrone(updated)
    }

    fun collectData(id: Int) {
        val current = drone(id) ?: return
        if (!current.canCollectData) return
        val updated = current.copy(
            cargo = (current.cargo + 25).coerceAtMost(100),
            battery = (current.battery - 15).coerceAtLeast(0),
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
    }
}
