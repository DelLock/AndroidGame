package com.example.androidgame

import androidx.lifecycle.ViewModel

data class DroneListUiState(
    val drones: List<Drone> = emptyList()
)

class DroneListViewModel(private val repo: DroneRepository) : ViewModel() {
    val uiState: DroneListUiState
        get() = DroneListUiState(drones = repo.drones)

    fun onAddDrone() = repo.addDrone()
    fun onDeleteDrone(id: Int) = repo.deleteDrone(id)
}
