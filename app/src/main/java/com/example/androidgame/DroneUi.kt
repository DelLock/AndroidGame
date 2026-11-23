package com.example.androidgame

import androidx.compose.ui.graphics.Color

object DroneColors {
    val Background = Color(0xFF0B0F14)        // Глубокий тёмный фон
    val Surface = Color(0xFF151B23)           // Карточки и контейнеры
    val SurfaceAlt = Color(0xFF1C2430)        // Выделенные карточки/поля
    val SurfaceBorder = Color(0xFF2B3648)     // Границы и разделители

    val Cyan = Color(0xFF4CC9F0)              // Неоново-голубой
    val CyanDark = Color(0xFF1A3B4D)          // Мягкий фон для Cyan элементов
    val Pink = Color(0xFFFF6B9D)              // Розовый акцент
    val PinkDark = Color(0xFF3D1E2D)          // Мягкий фон для Pink элементов
    val Good = Color(0xFF57CC99)              // Зеленый статус
    val GoodDark = Color(0xFF163326)          // Мягкий фон для Good
    val Danger = Color(0xFFFF5C5C)            // Опасность/Красный
    val DangerDark = Color(0xFF3D1C1C)        // Мягкий фон для Danger
    val Warning = Color(0xFFFFB347)           // Предупреждение/Оранжевый
    val WarningDark = Color(0xFF3A2814)       // Мягкий фон для Warning

    val TextPrimary = Color(0xFFE8F1F8)       // Основной текст
    val TextSecondary = Color(0xFF9AABB8)     // Вторичный текст
    val Muted = Color(0xFF6B7C8A)             // Приглушённый текст
}

data class StatusStyle(
    val cardColor: Color,
    val text: String,
    val accent: Color,
    val badgeBg: Color,
)

fun Drone.Status.style(): StatusStyle = when (this) {
    Drone.Status.NORMAL -> StatusStyle(
        cardColor = DroneColors.Surface,
        text = "Системы в норме",
        accent = DroneColors.Good,
        badgeBg = DroneColors.GoodDark,
    )
    Drone.Status.ON_MISSION -> StatusStyle(
        cardColor = DroneColors.SurfaceAlt,
        text = "НА МИССИИ",
        accent = DroneColors.Cyan,
        badgeBg = DroneColors.CyanDark,
    )
    Drone.Status.OVERHEATED -> StatusStyle(
        cardColor = DroneColors.Surface,
        text = "КРИТИЧЕСКИЙ ПЕРЕГРЕВ",
        accent = DroneColors.Danger,
        badgeBg = DroneColors.DangerDark,
    )
    Drone.Status.DEPLETED -> StatusStyle(
        cardColor = DroneColors.Surface,
        text = "НИЗКИЙ ЗАРЯД",
        accent = DroneColors.Warning,
        badgeBg = DroneColors.WarningDark,
    )
    Drone.Status.FULL_CARGO -> StatusStyle(
        cardColor = DroneColors.Surface,
        text = "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО",
        accent = DroneColors.Pink,
        badgeBg = DroneColors.PinkDark,
    )
}
