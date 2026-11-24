package com.example.androidgame.domain.model

import com.example.androidgame.data.api.dto.CurrentWeatherDto
import kotlinx.serialization.Serializable

@Serializable
data class DroneTelemetry(
    val model: String,
    val firmware: String,
    val rangeKm: Int,
    val sensorType: String,
    val weather: CurrentWeatherDto,
    val notes: String,
    val isOnlineData: Boolean,
)
