package com.example.androidgame

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature") val temperature: Double = 20.0,
    @SerialName("windspeed") val windSpeed: Double = 5.0,
    @SerialName("weathercode") val weatherCode: Int = 0,
)

/** Ветер от этого значения (м/с) считается сильным. */
const val WIND_LIMIT_MS = 10.0

/** Сколько % заряда тратит сбор данных в обычную и в ветреную погоду. */
const val NORMAL_COLLECT_COST = 15
const val WINDY_COLLECT_COST = 25

/** Город, над которым "дежурит" дрон. Для него мы запрашиваем погоду. */
data class DroneBase(val city: String, val latitude: Double, val longitude: Double)

private val BASES = listOf(
    DroneBase("Москва", 55.75, 37.62),
    DroneBase("Майами", 25.76, -80.19),
    DroneBase("Токио", 35.68, 139.69),
    DroneBase("Рейкьявик", 64.15, -21.94),
    DroneBase("Дубай", 25.20, 55.27),
    DroneBase("Сидней", -33.87, 151.21),
    DroneBase("Берлин", 52.52, 13.41),
    DroneBase("Нью-Йорк", 40.71, -74.01),
)

fun baseFor(droneId: Int): DroneBase = BASES[Math.floorMod(droneId - 1, BASES.size)]

val CurrentWeatherDto.isWindy: Boolean get() = windSpeed >= WIND_LIMIT_MS

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Ясно"
    1, 2, 3 -> "Облачно"
    45, 48 -> "Туман"
    in 51..67 -> "Дождь"
    in 71..77 -> "Снег"
    in 80..82 -> "Ливень"
    in 85..86 -> "Снегопад"
    in 95..99 -> "Гроза"
    else -> "Переменно"
}
