package com.example.androidgame.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.domain.model.Drone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class DroneFilter { ALL, NORMAL, ON_MISSION, OVERHEATED, DEPLETED, FULL_CARGO }

data class DroneListUiState(
    val drones: List<Drone> = emptyList(),
    val credits: Int = 300,
    val searchQuery: String = "",
    val selectedFilter: DroneFilter = DroneFilter.ALL,
    val lastDeletedDroneName: String? = null,
    val undoTimerSeconds: Int = 0,
    val missionNoticeMessage: String? = null,
    val networkReachable: Boolean = true,
)

class DroneListViewModel(private val repo: DroneRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(DroneFilter.ALL)

    val uiState: StateFlow<DroneListUiState> = combine(
        repo.dronesFlow,
        repo.creditsFlow,
        repo.lastDeletedDroneFlow,
        repo.undoTimerSecondsFlow,
        repo.lastCompletedMissionMessageFlow
    ) { drones, credits, lastDeleted, undoTimer, missionNotice ->
        Triple(drones, credits, Triple(lastDeleted, undoTimer, missionNotice))
    }.combine(_searchQuery) { base, query ->
        Pair(base, query)
    }.combine(_selectedFilter) { (base, query), filter ->
        val drones = base.first
        val credits = base.second
        val lastDeleted = base.third.first
        val undoTimer = base.third.second
        val missionNotice = base.third.third

        val filteredBySearch = if (query.isBlank()) drones else drones.filter { it.name.contains(query, ignoreCase = true) }
        val filteredByStatus = when (filter) {
            DroneFilter.ALL -> filteredBySearch
            DroneFilter.NORMAL -> filteredBySearch.filter { it.status == Drone.Status.NORMAL }
            DroneFilter.ON_MISSION -> filteredBySearch.filter { it.status == Drone.Status.ON_MISSION }
            DroneFilter.OVERHEATED -> filteredBySearch.filter { it.status == Drone.Status.OVERHEATED }
            DroneFilter.DEPLETED -> filteredBySearch.filter { it.status == Drone.Status.DEPLETED }
            DroneFilter.FULL_CARGO -> filteredBySearch.filter { it.status == Drone.Status.FULL_CARGO }
        }
        DroneListUiState(
            drones = filteredByStatus,
            credits = credits,
            searchQuery = query,
            selectedFilter = filter,
            lastDeletedDroneName = lastDeleted?.name,
            undoTimerSeconds = undoTimer,
            missionNoticeMessage = missionNotice,
        )
    }.combine(repo.networkReachableFlow) { listState, reachable ->
        listState.copy(networkReachable = reachable)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DroneListUiState(
            drones = repo.drones,
            credits = repo.credits,
            networkReachable = repo.networkReachableFlow.value,
        )
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChange(newFilter: DroneFilter) {
        _selectedFilter.value = newFilter
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

    fun onNetworkRestored() = repo.resetNetworkStatus()

    fun onHibernateAll() = repo.hibernateAll()
    fun onTransmitAll() = repo.transmitAll()
}
