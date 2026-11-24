package com.example.androidgame.domain.model

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

const val WIND_LIMIT_MS = 10.0
const val NORMAL_COLLECT_COST = 15
const val WINDY_COLLECT_COST = 25

fun isWindy(windSpeedKmH: Double): Boolean = (windSpeedKmH / 3.6) >= WIND_LIMIT_MS
fun isStormy(weatherCode: Int): Boolean = weatherCode in 95..99 || weatherCode == 82

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
