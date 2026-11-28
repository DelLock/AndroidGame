package com.example.androidgame.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidgame.data.DroneNetworkException
import com.example.androidgame.data.DroneNotFoundException
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.DroneTelemetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Loaded(val telemetry: DroneTelemetry) : WeatherState
    data class Cached(val telemetry: DroneTelemetry, val reason: String, val canRetry: Boolean = true) : WeatherState
    data class Failed(val message: String, val canRetry: Boolean) : WeatherState
}

sealed interface DroneDetailUiState {
    data class Content(
        val drone: Drone,
        val weatherState: WeatherState,
        val credits: Int,
    ) : DroneDetailUiState
    data object NotFound : DroneDetailUiState
}

class DroneDetailViewModel(
    private val droneId: Int,
    private val repo: DroneRepository,
) : ViewModel() {

    private var weather: WeatherState by mutableStateOf(WeatherState.Loading)
    private var retryCount by mutableIntStateOf(0)
    private var seenRestoreCount by mutableIntStateOf(repo.networkRestoreCount)
    private var weatherJob: Job? = null

    // После возвращения сети старые попытки не считаются.
    private val usedRetries: Int
        get() = if (seenRestoreCount == repo.networkRestoreCount) retryCount else 0

    val uiState: DroneDetailUiState
        get() {
            val drone = repo.drone(droneId) ?: return DroneDetailUiState.NotFound
            return DroneDetailUiState.Content(drone, applyRetryLimit(weather), repo.credits)
        }

    init {
        loadWeather(forceNetwork = false)
    }

    fun onRefreshWeather() {
        if (weatherJob?.isActive == true || usedRetries >= MAX_MANUAL_RETRIES) return
        retryCount = usedRetries + 1
        seenRestoreCount = repo.networkRestoreCount
        loadWeather(forceNetwork = true)
    }

    fun onRename(newName: String) {
        repo.renameDrone(droneId, newName)
    }

    fun onSetBase(baseCity: String) {
        val drone = repo.drone(droneId) ?: return
        if (drone.status == Drone.Status.ON_MISSION) return
        repo.setDroneBase(droneId, baseCity)
        retryCount = 0
        weatherJob?.cancel()
        loadWeather(forceNetwork = false)
    }

    fun onCollectData() {
        val content = uiState as? DroneDetailUiState.Content
        val isWindy = when (val ws = content?.weatherState) {
            is WeatherState.Loaded -> ws.telemetry.weather.isWindy
            is WeatherState.Cached -> !ws.telemetry.isExpired && ws.telemetry.weather.isWindy
            else -> false
        }
        repo.collectData(droneId, isWindy)
    }

    fun onTransmitData() = repo.transmitData(droneId)

    fun onHibernate() = repo.hibernate(droneId)

    fun onReset() = repo.resetDrone(droneId)

    fun onStartMission(durationSeconds: Int) {
        repo.startMission(id = droneId, durationSeconds = durationSeconds)
    }

    fun onUpgradeBattery() = repo.upgradeBattery(droneId)

    fun onUpgradeCargo() = repo.upgradeCargo(droneId)

    fun onUpgradeCooling() = repo.upgradeCooling(droneId)

    fun onDelete() = repo.deleteDrone(droneId)

    private fun loadWeather(forceNetwork: Boolean) {
        if (repo.drone(droneId) == null) return
        weather = WeatherState.Loading
        weatherJob = viewModelScope.launch {
            try {
                val telemetry = repo.fetchTelemetry(droneId, forceRefresh = forceNetwork)
                weather = if (telemetry.isOnlineData) {
                    retryCount = 0
                    WeatherState.Loaded(telemetry)
                } else {
                    WeatherState.Cached(
                        telemetry = telemetry,
                        reason = if (telemetry.canRetry) "Загружено из кэша" else "Повторный запрос не поможет",
                        canRetry = telemetry.canRetry,
                    )
                }
            } catch (e: DroneNotFoundException) {
                // Дрон удалён: uiState сам вернёт NotFound.
            } catch (e: DroneNetworkException) {
                weather = WeatherState.Failed(e.reason.userMessage, e.canRetry)
            }
        }
    }

    private fun applyRetryLimit(state: WeatherState): WeatherState {
        if (usedRetries < MAX_MANUAL_RETRIES) return state
        return when (state) {
            is WeatherState.Cached -> state.copy(canRetry = false, reason = "Лимит повторов исчерпан")
            is WeatherState.Failed -> state.copy(
                canRetry = false,
                message = "${state.message}. Лимит повторов исчерпан",
            )
            else -> state
        }
    }

    private companion object {
        const val MAX_MANUAL_RETRIES = 3
    }
}
