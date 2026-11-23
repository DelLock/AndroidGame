package com.example.androidgame.data.api.dto

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

const val WIND_LIMIT_MS = 10.0

const val NORMAL_COLLECT_COST = 15
const val WINDY_COLLECT_COST = 25

data class DroneBase(val city: String, val latitude: Double, val longitude: Double)

val ALL_BASES = listOf(
    DroneBase("Москва", 55.75, 37.62),
    DroneBase("Майами", 25.76, -80.19),
    DroneBase("Токио", 35.68, 139.69),
    DroneBase("Рейкьявик", 64.15, -21.94),
    DroneBase("Дубай", 25.20, 55.27),
    DroneBase("Сидней", -33.87, 151.21),
    DroneBase("Берлин", 52.52, 13.41),
    DroneBase("Нью-Йорк", 40.71, -74.01),
)

fun baseFor(city: String): DroneBase = ALL_BASES.find { it.city.equals(city, ignoreCase = true) } ?: ALL_BASES.first()

val CurrentWeatherDto.isWindy: Boolean get() = windSpeedMs >= WIND_LIMIT_MS

val CurrentWeatherDto.isStormy: Boolean get() = weatherCode in 95..99 || weatherCode == 82

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Ясно"
    1, 2, 3 -> "Облачно"
    45, 48 -> "Туман"
    in 51..67 -> "Дождь"
    in 71..77 -> "Снег"
    in 80..82 -> "Ливень"
    in 85..86 -> "Снегопад"
    in 95..99 -> "Гроза / Шторм"
    else -> "Переменно"
}
