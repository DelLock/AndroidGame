package com.example.androidgame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DroneFilter { ALL, NORMAL, ON_MISSION, OVERHEATED, DEPLETED, FULL_CARGO }

data class DroneListUiState(
    val drones: List<Drone> = emptyList(),
    val credits: Int = 250,
    val searchQuery: String = "",
    val selectedFilter: DroneFilter = DroneFilter.ALL,
    val lastDeletedDroneName: String? = null,
)

class DroneListViewModel(private val repo: DroneRepository) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf(DroneFilter.ALL)
        private set

    val uiState: DroneListUiState
        get() {
            val all = repo.drones
            val filteredBySearch = if (searchQuery.isBlank()) all else all.filter { it.name.contains(searchQuery, ignoreCase = true) }
            val filteredByStatus = when (selectedFilter) {
                DroneFilter.ALL -> filteredBySearch
                DroneFilter.NORMAL -> filteredBySearch.filter { it.status == Drone.Status.NORMAL }
                DroneFilter.ON_MISSION -> filteredBySearch.filter { it.status == Drone.Status.ON_MISSION }
                DroneFilter.OVERHEATED -> filteredBySearch.filter { it.status == Drone.Status.OVERHEATED }
                DroneFilter.DEPLETED -> filteredBySearch.filter { it.status == Drone.Status.DEPLETED }
                DroneFilter.FULL_CARGO -> filteredBySearch.filter { it.status == Drone.Status.FULL_CARGO }
            }
            return DroneListUiState(
                drones = filteredByStatus,
                credits = repo.credits,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                lastDeletedDroneName = repo.lastDeletedDrone?.name,
            )
        }

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                repo.tickMissions()
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun onFilterChange(newFilter: DroneFilter) {
        selectedFilter = newFilter
    }

    fun onAddDrone(name: String? = null) = repo.addDrone(name)
    fun onDeleteDrone(id: Int) = repo.deleteDrone(id)
    fun onRestoreDeletedDrone() = repo.restoreLastDeletedDrone()
    fun onHibernateAll() = repo.hibernateAll()
    fun onTransmitAll() = repo.transmitAll()
}
