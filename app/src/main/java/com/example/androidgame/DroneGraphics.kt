package com.example.androidgame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Векторная отрисовка дрона через Canvas.
 * Визуальные отличия:
 * - OVERHEATED: асимметричный неровный клубящийся дым над раскаленным корпусом.
 * - FULL_CARGO: зеленый округленный (шарообразный) корпус без сторонних фоновых фигур.
 * - DEPLETED: спит — над дроном поднимаются синие буквы Z.
 * - NORMAL: четкий металлический корпус с неоново-голубым сенсором.
 */
@Composable
fun DroneImage(
    status: Drone.Status,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        Drone.Status.NORMAL -> Color(0xFF1976D2)
        Drone.Status.OVERHEATED -> Color(0xFFD32F2F)
        Drone.Status.DEPLETED -> Color(0xFFF57C00)
        Drone.Status.FULL_CARGO -> Color(0xFF388E3C)
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // 1. НЕРОВНЫЙ АСИММЕТРИЧНЫЙ ДЫМ ПРИ ПЕРЕГРЕВЕ (OVERHEATED)
            if (status == Drone.Status.OVERHEATED) {
                drawCircle(color = Color(0x559E9E9E), radius = w * 0.11f, center = Offset(cx - w * 0.08f, cy - h * 0.28f))
                drawCircle(color = Color(0x66B0BEC5), radius = w * 0.15f, center = Offset(cx + w * 0.05f, cy - h * 0.33f))
                drawCircle(color = Color(0x44CFD8DC), radius = w * 0.18f, center = Offset(cx - w * 0.14f, cy - h * 0.38f))
                drawCircle(color = Color(0x5590A4AE), radius = w * 0.13f, center = Offset(cx + w * 0.12f, cy - h * 0.42f))
                drawCircle(color = Color(0x33ECEFF1), radius = w * 0.22f, center = Offset(cx - w * 0.02f, cy - h * 0.48f))
            }

            // 2. СИНИЕ БУКВЫ "Z" ПРИ НИЗКОМ ЗАРЯДЕ / СПЯЩЕМ РЕЖИМЕ (DEPLETED)
            if (status == Drone.Status.DEPLETED) {
                drawZ(
                    color = Color(0xFF03A9F4),
                    topLeft = Offset(cx + w * 0.10f, cy - h * 0.35f),
                    zSize = w * 0.10f,
                    strokeWidth = w * 0.035f
                )
                drawZ(
                    color = Color(0xFF4FC3F7),
                    topLeft = Offset(cx + w * 0.22f, cy - h * 0.50f),
                    zSize = w * 0.15f,
                    strokeWidth = w * 0.045f
                )
            }

            val armLength = w * 0.32f
            val rotorRadius = w * 0.12f

            // 3. Лучи квадрокоптера
            val strokeWidth = w * 0.045f
            val armColor = if (status == Drone.Status.DEPLETED) Color(0xFF37474F) else Color(0xFF455A64)

            drawLine(
                color = armColor,
                start = Offset(cx - armLength, cy - armLength),
                end = Offset(cx + armLength, cy + armLength),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = armColor,
                start = Offset(cx + armLength, cy - armLength),
                end = Offset(cx - armLength, cy + armLength),
                strokeWidth = strokeWidth
            )

            // 4. Пропеллеры / Моторы
            val rotorCenters = listOf(
                Offset(cx - armLength, cy - armLength),
                Offset(cx + armLength, cy - armLength),
                Offset(cx - armLength, cy + armLength),
                Offset(cx + armLength, cy + armLength)
            )

            for (center in rotorCenters) {
                drawCircle(
                    color = Color(0xFF263238),
                    radius = rotorRadius,
                    center = center,
                    style = Stroke(width = w * 0.025f)
                )
                drawLine(
                    color = if (status == Drone.Status.DEPLETED) Color(0xFF607D8B) else Color(0xFF90A4AE),
                    start = Offset(center.x - rotorRadius * 0.85f, center.y),
                    end = Offset(center.x + rotorRadius * 0.85f, center.y),
                    strokeWidth = w * 0.03f
                )
            }

            // 5. Корпус дрона
            // ПРИ FULL_CARGO: корпус просто становится зелёным и полностью округлённым
            val isFullCargo = status == Drone.Status.FULL_CARGO
            val bodySize = if (isFullCargo) w * 0.44f else w * 0.38f
            val cornerPercent = if (isFullCargo) 0.5f else 0.08f

            val bodyColor = when (status) {
                Drone.Status.OVERHEATED -> Color(0xFF3E2723)
                Drone.Status.DEPLETED -> Color(0xFF1C2833)
                Drone.Status.FULL_CARGO -> Color(0xFF2E7D32)
                Drone.Status.NORMAL -> Color(0xFF263238)
            }

            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(cx - bodySize / 2f, cy - bodySize / 2f),
                size = Size(bodySize, bodySize),
                cornerRadius = CornerRadius(bodySize * cornerPercent, bodySize * cornerPercent)
            )

            // Металлическая окантовка корпуса
            val rimColor = when (status) {
                Drone.Status.OVERHEATED -> Color(0xFFFF5722)
                Drone.Status.FULL_CARGO -> Color(0xFF81C784)
                else -> Color(0xFF78909C)
            }

            drawRoundRect(
                color = rimColor,
                topLeft = Offset(cx - bodySize / 2f, cy - bodySize / 2f),
                size = Size(bodySize, bodySize),
                cornerRadius = CornerRadius(bodySize * cornerPercent, bodySize * cornerPercent),
                style = Stroke(width = w * 0.025f)
            )

            // 6. Оптический сенсор / Объектив камеры
            val lensRadius = w * 0.09f
            val lensGlow = when (status) {
                Drone.Status.OVERHEATED -> Color(0xFFFF3D00)
                Drone.Status.DEPLETED -> Color(0xFF546E7A)
                Drone.Status.FULL_CARGO -> Color(0xFF00E676)
                Drone.Status.NORMAL -> Color(0xFF00E5FF)
            }

            drawCircle(
                color = Color(0xFF000000),
                radius = lensRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = lensGlow,
                radius = lensRadius * 0.5f,
                center = Offset(cx, cy)
            )

            // 7. Светодиодный индикатор статуса систем
            drawCircle(
                color = statusColor,
                radius = w * 0.045f,
                center = Offset(cx, cy - bodySize * 0.35f)
            )
        }
    }
}

/**
 * Векторное рисование буквы "Z" для спящего дрона
 */
private fun DrawScope.drawZ(color: Color, topLeft: Offset, zSize: Float, strokeWidth: Float) {
    drawLine(color, start = topLeft, end = Offset(topLeft.x + zSize, topLeft.y), strokeWidth = strokeWidth)
    drawLine(color, start = Offset(topLeft.x + zSize, topLeft.y), end = Offset(topLeft.x, topLeft.y + zSize), strokeWidth = strokeWidth)
    drawLine(color, start = Offset(topLeft.x, topLeft.y + zSize), end = Offset(topLeft.x + zSize, topLeft.y + zSize), strokeWidth = strokeWidth)
}

/**
 * Холдеры графика/иконок состояний (перегрев, разрядка, переполнение, норма)
 * Нарисованы векторно через Canvas без использования эмодзи.
 */
@Composable
fun StatusImage(
    status: Drone.Status,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            when (status) {
                Drone.Status.OVERHEATED -> {
                    val red = Color(0xFFD32F2F)
                    drawCircle(color = Color(0xFFFFEBEE), radius = w * 0.45f, center = Offset(cx, cy))
                    drawCircle(color = red, radius = w * 0.45f, center = Offset(cx, cy), style = Stroke(width = w * 0.06f))

                    val bulbRadius = w * 0.16f
                    drawCircle(color = red, radius = bulbRadius, center = Offset(cx, cy + h * 0.15f))
                    val stemWidth = w * 0.14f
                    val stemHeight = h * 0.35f
                    drawRoundRect(
                        color = red,
                        topLeft = Offset(cx - stemWidth / 2f, cy - h * 0.25f),
                        size = Size(stemWidth, stemHeight),
                        cornerRadius = CornerRadius(stemWidth / 2f, stemWidth / 2f)
                    )
                }

                Drone.Status.DEPLETED -> {
                    val orange = Color(0xFFE65100)
                    drawCircle(color = Color(0xFFFFF3E0), radius = w * 0.45f, center = Offset(cx, cy))
                    drawCircle(color = orange, radius = w * 0.45f, center = Offset(cx, cy), style = Stroke(width = w * 0.06f))

                    val bw = w * 0.42f
                    val bh = h * 0.28f
                    val bx = cx - bw / 2f
                    val by = cy - bh / 2f
                    drawRoundRect(
                        color = orange,
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                        style = Stroke(width = w * 0.05f)
                    )
                    drawRect(
                        color = orange,
                        topLeft = Offset(cx + bw / 2f, cy - bh * 0.22f),
                        size = Size(w * 0.05f, bh * 0.44f)
                    )
                    drawRect(
                        color = orange,
                        topLeft = Offset(bx + w * 0.05f, by + h * 0.04f),
                        size = Size(bw * 0.25f, bh - h * 0.08f)
                    )
                }

                Drone.Status.FULL_CARGO -> {
                    val green = Color(0xFF2E7D32)
                    drawCircle(color = Color(0xFFE8F5E9), radius = w * 0.45f, center = Offset(cx, cy))
                    drawCircle(color = green, radius = w * 0.45f, center = Offset(cx, cy), style = Stroke(width = w * 0.06f))

                    val dw = w * 0.38f
                    val dh = h * 0.42f
                    val dx = cx - dw / 2f
                    val dy = cy - dh / 2f
                    drawRoundRect(
                        color = green,
                        topLeft = Offset(dx, dy),
                        size = Size(dw, dh),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                    drawLine(color = Color.White, start = Offset(dx + dw * 0.2f, dy + dh * 0.3f), end = Offset(dx + dw * 0.8f, dy + dh * 0.3f), strokeWidth = w * 0.04f)
                    drawLine(color = Color.White, start = Offset(dx + dw * 0.2f, dy + dh * 0.6f), end = Offset(dx + dw * 0.8f, dy + dh * 0.6f), strokeWidth = w * 0.04f)
                }

                Drone.Status.NORMAL -> {
                    val blue = Color(0xFF1976D2)
                    drawCircle(color = Color(0xFFE3F2FD), radius = w * 0.45f, center = Offset(cx, cy))
                    drawCircle(color = blue, radius = w * 0.45f, center = Offset(cx, cy), style = Stroke(width = w * 0.06f))

                    drawCircle(color = blue, radius = w * 0.22f, center = Offset(cx, cy))
                }
            }
        }
    }
}
