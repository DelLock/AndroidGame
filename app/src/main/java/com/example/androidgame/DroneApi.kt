package com.example.androidgame

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class DroneTelemetryDto(
    val model: String = "X-Scout Mk-IV",
    @SerialName("firmware") val firmware: String = "v3.12.8-cyber",
    @SerialName("range_km") val rangeKm: Int = 120,
    @SerialName("sensor_type") val sensorType: String = "LiDAR / Quantum Optical",
    val notes: String = "Спецификация телеметрии успешно загружена по сети с сервера штаба.",
)

interface DroneApi {
    @GET("telemetry-{id}.json")
    suspend fun getTelemetry(@Path("id") id: Int): DroneTelemetryDto
}
