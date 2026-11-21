package com.example.androidgame

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OpenMeteoResponseDto(
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto = CurrentWeatherDto()
)

@Serializable
data class DroneTelemetryDto(
    val model: String = "X-Scout Mk-IV",
    @SerialName("firmware") val firmware: String = "v3.12.8-cyber",
    @SerialName("range_km") val rangeKm: Int = 120,
    @SerialName("sensor_type") val sensorType: String = "LiDAR / Quantum Optical",
    val weather: CurrentWeatherDto = CurrentWeatherDto(),
    val notes: String = "Спецификация телеметрии успешно загружена по сети с сервера штаба.",
    val isOnlineData: Boolean = true,
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
    ): OpenMeteoResponseDto
}
