package com.example.androidgame.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidgame.data.DroneNetworkException
import com.example.androidgame.data.DroneNotFoundException
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.DroneTelemetry
import kotlinx.coroutines.CancellationException
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

    var uiState: DroneDetailUiState by mutableStateOf(initialState())
        private set

    private var weatherJob: Job? = null
    private var retryCount = 0

    init {
        val initialDrone = repo.drone(droneId)
        if (initialDrone != null) {
            loadWeather(forceNetwork = false)
        }

        viewModelScope.launch {
            repo.dronesFlow.collect { drones ->
                val updatedDrone = drones.find { it.id == droneId }
                val current = uiState
                if (updatedDrone == null) {
                    uiState = DroneDetailUiState.NotFound
                } else if (current is DroneDetailUiState.Content) {
                    uiState = current.copy(drone = updatedDrone)
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

        viewModelScope.launch {
            repo.networkReachableFlow.collect { reachable ->
                if (reachable) {
                    retryCount = 0
                    val current = uiState
                    if (current is DroneDetailUiState.Content) {
                        when (val ws = current.weatherState) {
                            is WeatherState.Cached -> if (!ws.canRetry) updateWeatherState(ws.copy(canRetry = true))
                            is WeatherState.Failed -> if (!ws.canRetry) updateWeatherState(ws.copy(canRetry = true))
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    fun onRefreshWeather() {
        if (weatherJob?.isActive == true) return
        if (retryCount >= 3) {
            val current = uiState
            if (current is DroneDetailUiState.Content && current.weatherState is WeatherState.Cached) {
                updateWeatherState(current.weatherState.copy(reason = "Лимит повторов превышен", canRetry = false))
            } else {
                updateWeatherState(
                    WeatherState.Failed(
                        message = "Превышен лимит попыток подключения.",
                        canRetry = false,
                    )
                )
            }
            return
        }
        retryCount++
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
            is WeatherState.Cached -> ws.telemetry.weather.isWindy
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

    private fun initialState(): DroneDetailUiState {
        val drone = repo.drone(droneId) ?: return DroneDetailUiState.NotFound
        return DroneDetailUiState.Content(
            drone = drone,
            weatherState = WeatherState.Loading,
            credits = repo.credits,
        )
    }

    private fun loadWeather(forceNetwork: Boolean) {
        weatherJob = viewModelScope.launch {
            val droneAtStart = repo.drone(droneId) ?: return@launch
            val requestedCity = droneAtStart.baseCity

            val previousCached = (uiState as? DroneDetailUiState.Content)?.weatherState as? WeatherState.Cached

            updateWeatherState(WeatherState.Loading)

            try {
                val telemetry = repo.fetchTelemetry(droneId, forceRefresh = forceNetwork)
                val currentDrone = repo.drone(droneId) ?: return@launch
                if (currentDrone.baseCity != requestedCity) return@launch

                if (telemetry.isOnlineData) retryCount = 0
                val newWeatherState = if (telemetry.isOnlineData) {
                    WeatherState.Loaded(telemetry)
                } else {
                    WeatherState.Cached(telemetry, reason = "Загружено из кэша")
                }
                updateWeatherState(newWeatherState)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DroneNotFoundException) {
                uiState = DroneDetailUiState.NotFound
            } catch (e: DroneNetworkException) {
                val currentDrone = repo.drone(droneId) ?: return@launch
                if (currentDrone.baseCity != requestedCity) return@launch

                val limitReached = retryCount >= 3
                if (limitReached && previousCached != null) {
                    updateWeatherState(
                        previousCached.copy(
                            reason = "Превышен лимит попыток подключения.",
                            canRetry = false,
                        )
                    )
                } else {
                    updateWeatherState(
                        WeatherState.Failed(
                            message = if (limitReached) "Превышен лимит попыток подключения." else e.reason.userMessage,
                            canRetry = e.canRetry && !limitReached,
                        )
                    )
                }
            }
        }
    }

    private fun updateWeatherState(newWeatherState: WeatherState) {
        val current = uiState
        if (current is DroneDetailUiState.Content) {
            uiState = current.copy(weatherState = newWeatherState)
        }
    }
}
