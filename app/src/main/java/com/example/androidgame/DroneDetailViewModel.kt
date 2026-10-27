package com.example.androidgame

import androidx.lifecycle.ViewModel

data class DroneDetailUiState(
    val drone: Drone?
)

class DroneDetailViewModel(
    private val droneId: Int,
    private val repo: DroneRepository
) : ViewModel() {
    val uiState: DroneDetailUiState
        get() = DroneDetailUiState(drone = repo.drone(droneId))

    fun onCollectData() = repo.collectData(droneId)
    fun onTransmitData() = repo.transmitData(droneId)
    fun onHibernate() = repo.hibernate(droneId)
    fun onReset() = repo.resetDrone(droneId)
    fun onDelete() = repo.deleteDrone(droneId)
}
