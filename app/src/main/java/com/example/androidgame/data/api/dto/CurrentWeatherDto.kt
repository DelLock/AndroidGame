package com.example.androidgame.data.api.dto

import com.example.androidgame.domain.model.isStormy
import com.example.androidgame.domain.model.isWindy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature") val temperature: Double,
    @SerialName("windspeed") val windSpeedKmH: Double,
    @SerialName("weathercode") val weatherCode: Int,
) {
    val windSpeedMs: Double get() = windSpeedKmH / 3.6
}

val CurrentWeatherDto.isWindy: Boolean get() = isWindy(windSpeedKmH)
val CurrentWeatherDto.isStormy: Boolean get() = isStormy(weatherCode)

@Serializable
data class CachedWeather(
    val weather: CurrentWeatherDto,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val isExpired: Boolean get() = (System.currentTimeMillis() - timestamp) > 2 * 60 * 60 * 1000L // 2 hours
}
