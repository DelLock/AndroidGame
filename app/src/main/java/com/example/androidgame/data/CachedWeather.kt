package com.example.androidgame.data

import com.example.androidgame.data.api.dto.CurrentWeatherDto
import kotlinx.serialization.Serializable

@Serializable
data class CachedWeather(
    val weather: CurrentWeatherDto,
    val timestamp: Long,
) {
    val isFresh: Boolean get() = (System.currentTimeMillis() - timestamp) < 10 * 60 * 1000L // 10 minutes
    val isExpired: Boolean get() = (System.currentTimeMillis() - timestamp) > 2 * 60 * 60 * 1000L // 2 hours
}
