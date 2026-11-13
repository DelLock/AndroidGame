package com.example.androidgame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface DroneDetailUiState {
    data object Loading : DroneDetailUiState
    data class Content(
        val drone: Drone,
        val telemetry: DroneTelemetryDto,
    ) : DroneDetailUiState
    data object NotFound : DroneDetailUiState
    data class Error(val canRetry: Boolean) : DroneDetailUiState
}

class DroneDetailViewModel(
    private val droneId: Int,
    private val repo: DroneRepository,
) : ViewModel() {

    var uiState: DroneDetailUiState by mutableStateOf(DroneDetailUiState.Loading)
        private set

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onRetry() = load()

    fun onRename(newName: String) {
        repo.renameDrone(droneId, newName)
        refreshState()
    }

    fun onCollectData() {
        val isWindy = (uiState as? DroneDetailUiState.Content)?.telemetry?.weather?.isWindy ?: false
        repo.collectData(droneId, isWindy)
        refreshState()
    }

    fun onTransmitData() {
        repo.transmitData(droneId)
        refreshState()
    }

    fun onHibernate() {
        repo.hibernate(droneId)
        refreshState()
    }

    fun onReset() {
        repo.resetDrone(droneId)
        refreshState()
    }

    fun onDelete() = repo.deleteDrone(droneId)

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            uiState = DroneDetailUiState.Loading
            uiState = fetchState()
        }
    }

    private fun refreshState() {
        val current = uiState
        if (current is DroneDetailUiState.Content) {
            val updatedDrone = repo.drone(droneId)
            uiState = if (updatedDrone == null) {
                DroneDetailUiState.NotFound
            } else {
                current.copy(drone = updatedDrone)
            }
        }
    }

    private suspend fun fetchState(): DroneDetailUiState {
        val drone = repo.drone(droneId) ?: return DroneDetailUiState.NotFound
        return try {
            val telemetry = repo.fetchTelemetry(droneId)
            DroneDetailUiState.Content(drone, telemetry)
        } catch (_: DroneNotFoundException) {
            DroneDetailUiState.NotFound
        } catch (e: DroneNetworkException) {
            DroneDetailUiState.Error(e.canRetry)
        }
    }
}
