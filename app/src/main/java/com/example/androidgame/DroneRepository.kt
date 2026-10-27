package com.example.androidgame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DroneRepository {
    var drones by mutableStateOf(startDrones())
        private set

    private var nextId = (drones.maxOfOrNull { it.id } ?: 0) + 1

    fun drone(id: Int): Drone? = drones.find { it.id == id }

    fun addDrone() {
        val newDrone = Drone(
            id = nextId,
            name = "Дрон-разведчик X-%02d".format(nextId),
            battery = 100,
            cargo = 0,
            temperature = 20
        )
        nextId++
        drones += newDrone
    }

    fun deleteDrone(id: Int) {
        val target = drone(id) ?: return
        drones -= target
    }

    fun collectData(id: Int) {
        val current = drone(id) ?: return
        if (!current.canCollectData) return
        val updated = current.copy(
            cargo = (current.cargo + 25).coerceAtMost(100),
            battery = (current.battery - 15).coerceAtLeast(0),
            temperature = (current.temperature + 30).coerceAtMost(100)
        )
        updateDrone(updated)
    }

    fun transmitData(id: Int) {
        val current = drone(id) ?: return
        if (!current.canTransmitData) return
        val updated = current.copy(
            cargo = 0,
            battery = (current.battery - 20).coerceAtLeast(0)
        )
        updateDrone(updated)
    }

    fun hibernate(id: Int) {
        val current = drone(id) ?: return
        if (!current.canHibernate) return
        val updated = current.copy(
            battery = 100,
            temperature = 20
        )
        updateDrone(updated)
    }

    fun resetDrone(id: Int) {
        val current = drone(id) ?: return
        val updated = current.copy(
            battery = 100,
            cargo = 0,
            temperature = 20
        )
        updateDrone(updated)
    }

    private fun updateDrone(updated: Drone) {
        drones = drones.map { if (it.id == updated.id) updated else it }
    }
}
