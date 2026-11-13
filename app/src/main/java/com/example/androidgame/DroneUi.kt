package com.example.androidgame

import androidx.compose.ui.graphics.Color

/** Единая палитра приложения (раньше цвета были разбросаны по файлам). */
object DroneColors {
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val SurfaceAlt = Color(0xFF21262D)
    val Cyan = Color(0xFF00E5FF)
    val Pink = Color(0xFFFF007F)
    val Muted = Color(0xFF8B949E)
    val Danger = Color(0xFFFF1744)
    val Good = Color(0xFF00E676)
}

/** Оформление карточки для каждого статуса дрона (раньше дублировалось в двух экранах). */
data class StatusStyle(val cardColor: Color, val text: String, val accent: Color)

fun Drone.Status.style(): StatusStyle = when (this) {
    Drone.Status.NORMAL -> StatusStyle(Color(0xFF161B22), "Системы в норме", Color(0xFF00E5FF))
    Drone.Status.OVERHEATED -> StatusStyle(Color(0xFF2D1214), "КРИТИЧЕСКИЙ ПЕРЕГРЕВ", Color(0xFFFF1744))
    Drone.Status.DEPLETED -> StatusStyle(Color(0xFF2D2012), "НИЗКИЙ ЗАРЯД", Color(0xFFFF9100))
    Drone.Status.FULL_CARGO -> StatusStyle(Color(0xFF122D1A), "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО", Color(0xFF00E676))
}
