package com.example.androidgame.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Weather(
    val temperature: Double,
    val windSpeedKmH: Double,
    val weatherCode: Int,
) {
    val windSpeedMs: Double get() = windSpeedKmH / 3.6
    val isWindy: Boolean get() = windSpeedMs >= WIND_LIMIT_MS
    val isStormy: Boolean get() = weatherCode in 95..99 || weatherCode == 82
}
