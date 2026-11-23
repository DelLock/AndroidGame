package com.example.androidgame.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponseDto(
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto
)

@Serializable
data class DroneTelemetryDto(
    val model: String,
    @SerialName("firmware") val firmware: String,
    @SerialName("range_km") val rangeKm: Int,
    @SerialName("sensor_type") val sensorType: String,
    val weather: CurrentWeatherDto,
    val notes: String,
    val isOnlineData: Boolean = true,
)
