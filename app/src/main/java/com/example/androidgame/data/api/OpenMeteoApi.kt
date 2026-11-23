package com.example.androidgame.data.api

import com.example.androidgame.data.api.dto.OpenMeteoResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
    ): OpenMeteoResponseDto
}
