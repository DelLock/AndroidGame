package com.example.androidgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneDetailScreen(
    state: DroneDetailUiState,
    onRetry: () -> Unit,
    onRename: (String) -> Unit,
    onCollectData: () -> Unit,
    onTransmitData: () -> Unit,
    onHibernate: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карточка дрона") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                    ) {
                        Text("Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22),
                    titleContentColor = Color(0xFF00E5FF),
                ),
            )
        },
        containerColor = Color(0xFF0D1117),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                DroneDetailUiState.Loading -> LoadingBox()
                is DroneDetailUiState.Content -> ContentBox(
                    drone = state.drone,
                    telemetry = state.telemetry,
                    onRename = onRename,
                    onCollectData = onCollectData,
                    onTransmitData = onTransmitData,
                    onHibernate = onHibernate,
                    onReset = onReset,
                    onDelete = onDelete,
                )
                DroneDetailUiState.NotFound -> NotFoundBox(onBack)
                is DroneDetailUiState.Error -> ErrorBox(state.canRetry, onRetry)
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Color(0xFF00E5FF))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Установка сетевого соединения с дроном...",
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContentBox(
    drone: Drone,
    telemetry: DroneTelemetryDto,
    onRename: (String) -> Unit,
    onCollectData: () -> Unit,
    onTransmitData: () -> Unit,
    onHibernate: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val (cardColor, statusText, statusColor) = when (drone.status) {
        Drone.Status.NORMAL -> Triple(Color(0xFF161B22), "Системы в норме", Color(0xFF00E5FF))
        Drone.Status.OVERHEATED -> Triple(Color(0xFF2D1214), "КРИТИЧЕСКИЙ ПЕРЕГРЕВ", Color(0xFFFF1744))
        Drone.Status.DEPLETED -> Triple(Color(0xFF2D2012), "НИЗКИЙ ЗАРЯД", Color(0xFFFF9100))
        Drone.Status.FULL_CARGO -> Triple(Color(0xFF122D1A), "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО", Color(0xFF00E676))
    }

    var isEditingName by remember { mutableStateOf(value = false) }
    var nameInput by remember(drone.name) { mutableStateOf(drone.name) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DroneImage(
                status = drone.status,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 12.dp),
            )

            if (isEditingName) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Имя дрона", color = Color(0xFF00E5FF)) },
                    )
                    Button(
                        onClick = {
                            onRename(nameInput)
                            isEditingName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    ) {
                        Text("ОК", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = drone.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { isEditingName = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                    ) {
                        Text("Переименовать", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusImage(
                    status = drone.status,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 6.dp),
                )
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontSize = 15.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailMetricBar("Заряд батареи", drone.battery, "%", Color(0xFF00E5FF))
            DetailMetricBar("Заполненность памяти", drone.cargo, "%", Color(0xFFFF007F))
            DetailMetricBar(
                "Температура ядра",
                drone.temperature,
                "°C",
                if (drone.temperature >= 80) Color(0xFFFF1744) else Color(0xFF00E676),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Сетевая телеметрия (API):",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                    )
                    Text("Модель: ${telemetry.model}", color = Color.White, fontSize = 12.sp)
                    Text("Прошивка: ${telemetry.firmware}", color = Color.White, fontSize = 12.sp)
                    Text("Дальность: ${telemetry.rangeKm} км", color = Color.White, fontSize = 12.sp)
                    Text("Сенсоры: ${telemetry.sensorType}", color = Color.White, fontSize = 12.sp)
                    Text(telemetry.notes, color = Color(0xFF8B949E), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onCollectData,
        enabled = drone.canCollectData,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
    ) {
        Text("Собрать данные окружения", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onTransmitData,
        enabled = drone.canTransmitData,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F), contentColor = Color.White),
    ) {
        Text("Передать данные на базу", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onHibernate,
        enabled = drone.canHibernate,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3), contentColor = Color.White),
    ) {
        Text("Гибернация (зарядка и охлаждение)", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
    ) {
        Text("Сброс до заводских настроек")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onDelete,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF1744)),
    ) {
        Text("Списать (удалить дрон)")
    }
}

@Composable
private fun NotFoundBox(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Дрон не найден или списан",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF1744),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)) {
            Text("Вернуться к списку", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorBox(canRetry: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (canRetry) "Не удалось подключиться к каналу связи дрона" else "Ошибка формата ответа сервера",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFF1744),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (canRetry) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
            ) {
                Text("Повторить загрузку", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailMetricBar(label: String, value: Int, unit: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 13.sp, color = Color(0xFFC9D1D9))
            Text("$value$unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (color == Color.Unspecified) Color(0xFF00E5FF) else color,
            trackColor = Color(0xFF21262D),
        )
    }
}
