package com.example.androidgame.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponseDto(
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto
)
