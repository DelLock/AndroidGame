package com.example.androidgame // Убедитесь, что пакет совпадает с вашим

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

enum class DroneState { NORMAL, OVERHEATED, DEPLETED, FULL_CARGO }

@Composable
fun DroneScreen() {
    var battery by remember { mutableIntStateOf(100) }
    var cargo by remember { mutableIntStateOf(0) }
    var temperature by remember { mutableIntStateOf(20) }

    val currentState = when {
        temperature >= 80 -> DroneState.OVERHEATED
        battery <= 10 -> DroneState.DEPLETED
        cargo >= 100 -> DroneState.FULL_CARGO
        else -> DroneState.NORMAL
    }

    val canCollectData = currentState != DroneState.OVERHEATED &&
            currentState != DroneState.DEPLETED &&
            cargo < 100

    val canTransmitData = cargo > 0 && battery > 20

    val canHibernate = battery < 100 || temperature > 20

    val (cardColor, statusText) = when (currentState) {
        DroneState.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to "Системы в норме"
        DroneState.OVERHEATED -> Color(0xFFFFCDD2) to "КРИТИЧЕСКИЙ ПЕРЕГРЕВ"
        DroneState.DEPLETED -> Color(0xFFFFF9C4) to "НИЗКИЙ ЗАРЯД"
        DroneState.FULL_CARGO -> Color(0xFFC8E6C9) to "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО"
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

                if (currentState == DroneState.OVERHEATED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Осторожно!", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(statusText, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(statusText, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Метрики
                MetricBar("Заряд батареи", battery, "%", Color.Blue)
                MetricBar("Заполненность памяти", cargo, "%", Color.DarkGray)
                MetricBar("Температура ядра", temperature, "°C", if (temperature >= 80) Color.Red else Color.Unspecified)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                cargo = (cargo + 25).coerceAtMost(100)
                battery = (battery - 15).coerceAtLeast(0)
                temperature = (temperature + 30).coerceAtMost(100)
            },
            enabled = canCollectData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Собрать данные окружения")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                cargo = 0
                battery = (battery - 20).coerceAtLeast(0)
            },
            enabled = canTransmitData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Передать данные на базу")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                battery = 100
                temperature = 20
            },
            enabled = canHibernate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Гибернация (зарядка и охлаждение)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                battery = 100
                cargo = 0
                temperature = 20
            },
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