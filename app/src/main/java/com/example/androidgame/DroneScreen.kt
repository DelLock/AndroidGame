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

class DroneState {
    var battery by mutableIntStateOf(100)
        private set

    var cargo by mutableIntStateOf(0)
        private set

    var temperature by mutableIntStateOf(20)
        private set

    val status: Status
        get() = when {
            temperature >= 80 -> Status.OVERHEATED
            battery <= 10 -> Status.DEPLETED
            cargo >= 100 -> Status.FULL_CARGO
            else -> Status.NORMAL
        }

    val canCollectData: Boolean
        get() = status != Status.OVERHEATED && status != Status.DEPLETED && cargo < 100

    val canTransmitData: Boolean
        get() = cargo > 0 && battery > 20

    val canHibernate: Boolean
        get() = battery < 100 || temperature > 20

    fun collectData() {
        cargo = (cargo + 25).coerceAtMost(100)
        battery = (battery - 15).coerceAtLeast(0)
        temperature = (temperature + 30).coerceAtMost(100)
    }

    fun transmitData() {
        cargo = 0
        battery = (battery - 20).coerceAtLeast(0)
    }

    fun hibernate() {
        battery = 100
        temperature = 20
    }

    fun reset() {
        battery = 100
        cargo = 0
        temperature = 20
    }

    enum class Status { NORMAL, OVERHEATED, DEPLETED, FULL_CARGO }
}

@Composable
fun DroneScreenRoot() {
    val drone = remember { DroneState() }
    DroneScreen(drone = drone)
}

@Composable
fun DroneScreen(
    drone: DroneState,
    modifier: Modifier = Modifier
) {
    val (cardColor, statusText) = when (drone.status) {
        DroneState.Status.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to "Системы в норме"
        DroneState.Status.OVERHEATED -> Color(0xFFFFCDD2) to "КРИТИЧЕСКИЙ ПЕРЕГРЕВ"
        DroneState.Status.DEPLETED -> Color(0xFFFFF9C4) to "НИЗКИЙ ЗАРЯД"
        DroneState.Status.FULL_CARGO -> Color(0xFFC8E6C9) to "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО"
    }

    Column(
        modifier = modifier
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

                if (drone.status == DroneState.Status.OVERHEATED) {
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
            onClick = { drone.collectData() },
            enabled = drone.canCollectData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Собрать данные окружения")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { drone.transmitData() },
            enabled = drone.canTransmitData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Передать данные на базу")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { drone.hibernate() },
            enabled = drone.canHibernate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Гибернация (зарядка и охлаждение)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { drone.reset() },
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
