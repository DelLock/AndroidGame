package com.example.androidgame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
data class Drone(
    val battery: Int = 100,
    val cargo: Int = 0,
    val temperature: Int = 20
) {
    val status: Status
        get() = when {
            temperature >= 80 -> Status.OVERHEATED
            battery <= 10 -> Status.DEPLETED
            cargo >= 100 -> Status.FULL_CARGO
            else -> Status.NORMAL
        }

    enum class Status { NORMAL, OVERHEATED, DEPLETED, FULL_CARGO }
}

@Composable
fun DroneScreenRoot() {
    var drone by remember { mutableStateOf(Drone()) }

    DroneScreen(
        drone = drone,
        onCollectData = {
            drone = drone.copy(
                cargo = (drone.cargo + 25).coerceAtMost(100),
                battery = (drone.battery - 15).coerceAtLeast(0),
                temperature = (drone.temperature + 30).coerceAtMost(100)
            )
        },
        onTransmitData = {
            drone = drone.copy(
                cargo = 0,
                battery = (drone.battery - 20).coerceAtLeast(0)
            )
        },
        onHibernate = {
            drone = drone.copy(
                battery = 100,
                temperature = 20
            )
        },
        onReset = {
            drone = Drone()
        }
    )
}

@Composable
fun DroneScreen(
    drone: Drone,
    onCollectData: () -> Unit,
    onTransmitData: () -> Unit,
    onHibernate: () -> Unit,
    onReset: () -> Unit
) {
    val canCollectData = drone.status != Drone.Status.OVERHEATED &&
            drone.status != Drone.Status.DEPLETED &&
            drone.cargo < 100

    val canTransmitData = drone.cargo > 0 && drone.battery > 20
    val canHibernate = drone.battery < 100 || drone.temperature > 20

    val (cardColor, statusText) = when (drone.status) {
        Drone.Status.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to "Системы в норме"
        Drone.Status.OVERHEATED -> Color(0xFFFFCDD2) to "КРИТИЧЕСКИЙ ПЕРЕГРЕВ"
        Drone.Status.DEPLETED -> Color(0xFFFFF9C4) to "НИЗКИЙ ЗАРЯД"
        Drone.Status.FULL_CARGO -> Color(0xFFC8E6C9) to "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Дрон-разведчик X-01",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (drone.status == Drone.Status.OVERHEATED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(statusText, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(statusText, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                MetricBar("Заряд батареи", drone.battery, "%", Color.Blue)
                MetricBar("Заполненность памяти", drone.cargo, "%", Color.DarkGray)
                MetricBar(
                    "Температура ядра",
                    drone.temperature,
                    "°C",
                    if (drone.temperature >= 80) Color.Red else Color.Unspecified
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCollectData,
            enabled = canCollectData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Собрать данные окружения")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onTransmitData,
            enabled = canTransmitData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Передать данные на базу")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onHibernate,
            enabled = canHibernate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Гибернация (зарядка и охлаждение)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
        ) {
            Text("Сброс до заводских настроек")
        }
    }
}

@Composable
fun MetricBar(label: String, value: Int, unit: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp)
            Text("$value$unit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color,
        )
    }
}
