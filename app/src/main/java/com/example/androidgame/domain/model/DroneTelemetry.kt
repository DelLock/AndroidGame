package com.example.androidgame.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DroneTelemetry(
    val model: String,
    val firmware: String,
    val rangeKm: Int,
    val sensorType: String,
    val weather: Weather,
    val notes: String,
    val isOnlineData: Boolean,
    val canRetry: Boolean = true,
    val isExpired: Boolean = false,
)
