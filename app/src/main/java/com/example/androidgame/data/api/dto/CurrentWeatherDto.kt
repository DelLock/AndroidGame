package com.example.androidgame.data.api.dto

import com.example.androidgame.domain.model.Weather
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature") val temperature: Double,
    @SerialName("windspeed") val windSpeedKmH: Double,
    @SerialName("weathercode") val weatherCode: Int,
)

fun CurrentWeatherDto.toDomain(): Weather = Weather(
    temperature = temperature,
    windSpeedKmH = windSpeedKmH,
    weatherCode = weatherCode,
)
