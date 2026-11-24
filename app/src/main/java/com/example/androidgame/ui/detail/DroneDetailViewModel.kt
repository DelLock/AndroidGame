package com.example.androidgame.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.data.api.dto.DroneNetworkException
import com.example.androidgame.data.api.dto.DroneNotFoundException
import com.example.androidgame.data.api.dto.isWindy
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.DroneTelemetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface DroneDetailUiState {
    data object Loading : DroneDetailUiState
    data class Content(
        val drone: Drone,
        val telemetry: DroneTelemetry,
        val credits: Int,
    ) : DroneDetailUiState
    data object NotFound : DroneDetailUiState
    data class Error(
        val canRetry: Boolean,
        val message: String = "Не удалось подключиться к каналу связи дрона",
    ) : DroneDetailUiState
}

class DroneDetailViewModel(
    private val droneId: Int,
    private val repo: DroneRepository,
) : ViewModel() {

    var uiState: DroneDetailUiState by mutableStateOf(DroneDetailUiState.Loading)
        private set

    private var loadJob: Job? = null
    private var retryCount = 0

    init {
        load()
        viewModelScope.launch {
            repo.dronesFlow.collect { drones ->
                val updatedDrone = drones.find { it.id == droneId }
                val current = uiState
                if (current is DroneDetailUiState.Content) {
                    if (updatedDrone == null) {
                        uiState = DroneDetailUiState.NotFound
                    } else {
                        uiState = current.copy(drone = updatedDrone, credits = repo.credits)
                    }
                }
            }
        }
        viewModelScope.launch {
            repo.creditsFlow.collect { credits ->
                val current = uiState
                if (current is DroneDetailUiState.Content) {
                    uiState = current.copy(credits = credits)
                }
            }
        }
    }

    fun onRetry() {
        if (retryCount >= 3) {
            uiState = DroneDetailUiState.Error(
                canRetry = false,
                message = "Превышен лимит попыток подключения.",
            )
            return
        }
        retryCount++
        load()
    }

    fun onRename(newName: String) {
        repo.renameDrone(droneId, newName)
    }

    fun onSetBase(baseCity: String) {
        repo.setDroneBase(droneId, baseCity)
        retryCount = 0
        load()
    }

    fun onCollectData() {
        val isWindy = (uiState as? DroneDetailUiState.Content)?.telemetry?.weather?.isWindy ?: false
        repo.collectData(droneId, isWindy)
    }

    fun onTransmitData() {
        repo.transmitData(droneId)
    }

    fun onHibernate() {
        repo.hibernate(droneId)
    }

    fun onReset() {
        repo.resetDrone(droneId)
    }

    fun onStartMission(durationSeconds: Int) {
        repo.startMission(id = droneId, durationSeconds = durationSeconds)
    }

    fun onUpgradeBattery() {
        repo.upgradeBattery(droneId)
    }

    fun onUpgradeCargo() {
        repo.upgradeCargo(droneId)
    }

    fun onUpgradeCooling() {
        repo.upgradeCooling(droneId)
    }

    fun onDelete() = repo.deleteDrone(droneId)

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            uiState = DroneDetailUiState.Loading
            val result = fetchState()
            if (result is DroneDetailUiState.Content) {
                retryCount = 0
            }
            uiState = result
        }
    }

    private suspend fun fetchState(): DroneDetailUiState {
        val drone = repo.drone(droneId) ?: return DroneDetailUiState.NotFound
        return try {
            val telemetry = repo.fetchTelemetry(droneId)
            DroneDetailUiState.Content(drone, telemetry, repo.credits)
        } catch (_: DroneNotFoundException) {
            DroneDetailUiState.NotFound
        } catch (e: DroneNetworkException) {
            DroneDetailUiState.Error(
                canRetry = e.canRetry,
                message = if (e.canRetry) "Не удалось подключиться к каналу связи дрона" else "Ошибка формата ответа сервера",
            )
        }
    }
}
