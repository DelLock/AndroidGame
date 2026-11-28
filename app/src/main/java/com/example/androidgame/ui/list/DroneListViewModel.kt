package com.example.androidgame.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.domain.model.Drone

enum class DroneFilter { ALL, NORMAL, ON_MISSION, OVERHEATED, DEPLETED, FULL_CARGO }

data class DroneListUiState(
    val drones: List<Drone> = emptyList(),
    val credits: Int = 300,
    val searchQuery: String = "",
    val selectedFilter: DroneFilter = DroneFilter.ALL,
    val lastDeletedDroneName: String? = null,
    val undoTimerSeconds: Int = 0,
    val missionNoticeMessage: String? = null,
    val isOnline: Boolean = true,
)

class DroneListViewModel(private val repo: DroneRepository) : ViewModel() {

    private var searchQuery by mutableStateOf("")
    private var selectedFilter by mutableStateOf(DroneFilter.ALL)

    // Собирается из репозитория при каждом чтении: Compose видит чтение и обновляет экран.
    val uiState: DroneListUiState
        get() = DroneListUiState(
            drones = repo.drones.filter { drone ->
                (searchQuery.isBlank() || drone.name.contains(searchQuery, ignoreCase = true)) &&
                    (selectedFilter == DroneFilter.ALL || drone.status.name == selectedFilter.name)
            },
            credits = repo.credits,
            searchQuery = searchQuery,
            selectedFilter = selectedFilter,
            lastDeletedDroneName = repo.lastDeletedDrone?.name,
            undoTimerSeconds = repo.undoTimerSeconds,
            missionNoticeMessage = repo.missionMessage,
            isOnline = repo.isOnline,
        )

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun onFilterChange(newFilter: DroneFilter) {
        selectedFilter = newFilter
    }

    fun onAddDrone(name: String? = null) = repo.addDrone(name)

    fun onDeleteDrone(id: Int) {
        repo.deleteDrone(id)
    }

    fun onRestoreDeletedDrone() {
        repo.restoreLastDeletedDrone()
    }

    fun onDismissMissionNotice() {
        repo.clearMissionNotice()
    }

    fun onHibernateAll() = repo.hibernateAll()
    fun onTransmitAll() = repo.transmitAll()
}
