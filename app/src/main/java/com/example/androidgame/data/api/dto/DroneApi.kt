package com.example.androidgame.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponseDto(
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto
)

@Serializable
data class DroneTelemetryDto(
    val model: String = "X-Scout Mk-IV",
    @SerialName("firmware") val firmware: String = "v3.12.8-cyber",
    @SerialName("range_km") val rangeKm: Int = 120,
    @SerialName("sensor_type") val sensorType: String = "LiDAR / Quantum Optical",
    val weather: CurrentWeatherDto = CurrentWeatherDto(20.0, 5.0, 0),
    val notes: String = "Спецификация телеметрии успешно загружена по сети с сервера штаба.",
    val isOnlineData: Boolean = true,
)
