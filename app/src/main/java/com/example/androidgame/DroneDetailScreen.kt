package com.example.androidgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneDetailScreen(
    state: DroneDetailUiState,
    onCollectData: () -> Unit,
    onTransmitData: () -> Unit,
    onHibernate: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.drone?.name ?: "Карточка дрона") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("← Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val drone = state.drone
            if (drone == null) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Дрон не найден или удалён",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Вернуться к списку")
                }
            } else {
                val (cardColor, statusText, statusColor) = when (drone.status) {
                    Drone.Status.NORMAL -> Triple(MaterialTheme.colorScheme.surfaceVariant, "Системы в норме", MaterialTheme.colorScheme.onSurfaceVariant)
                    Drone.Status.OVERHEATED -> Triple(Color(0xFFFFCDD2), "КРИТИЧЕСКИЙ ПЕРЕГРЕВ", Color.Red)
                    Drone.Status.DEPLETED -> Triple(Color(0xFFFFF9C4), "НИЗКИЙ ЗАРЯД", Color(0xFFE65100))
                    Drone.Status.FULL_CARGO -> Triple(Color(0xFFC8E6C9), "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО", Color(0xFF2E7D32))
                }

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
                            text = drone.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        DetailMetricBar("Заряд батареи", drone.battery, "%", Color.Blue)
                        DetailMetricBar("Заполненность памяти", drone.cargo, "%", Color.DarkGray)
                        DetailMetricBar(
                            "Температура ядра",
                            drone.temperature,
                            "°C",
                            if (drone.temperature >= 80) Color.Red else Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCollectData,
                    enabled = drone.canCollectData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Собрать данные окружения")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onTransmitData,
                    enabled = drone.canTransmitData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Передать данные на базу")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onHibernate,
                    enabled = drone.canHibernate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Гибернация (зарядка и охлаждение)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Blue)
                ) {
                    Text("Сброс до заводских настроек")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Списать (удалить дрон)")
                }
            }
        }
    }
}

@Composable
fun DetailMetricBar(label: String, value: Int, unit: String, color: Color) {
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

@Preview(showBackground = true)
@Composable
fun DroneDetailScreenPreview() {
    MaterialTheme {
        DroneDetailScreen(
            state = DroneDetailUiState(drone = startDrones().first()),
            onCollectData = {},
            onTransmitData = {},
            onHibernate = {},
            onReset = {},
            onDelete = {},
            onBack = {}
        )
    }
}
