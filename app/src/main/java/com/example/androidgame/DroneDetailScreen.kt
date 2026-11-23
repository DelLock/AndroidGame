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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
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
    onSetBase: (String) -> Unit,
    onCollectData: () -> Unit,
    onTransmitData: () -> Unit,
    onHibernate: () -> Unit,
    onReset: () -> Unit,
    onStartMission: (Int) -> Unit,
    onUpgradeBattery: () -> Unit,
    onUpgradeCargo: () -> Unit,
    onUpgradeCooling: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val soundManager = remember { SoundManager() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карточка дрона", fontWeight = FontWeight.Bold, color = DroneColors.TextPrimary) },
                navigationIcon = {
                    OutlinedButton(
                        onClick = {
                            soundManager.play(SoundManager.Effect.CLICK)
                            onBack()
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Назад", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DroneColors.Surface,
                ),
            )
        },
        containerColor = DroneColors.Background,
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
                    credits = state.credits,
                    soundManager = soundManager,
                    onRename = onRename,
                    onSetBase = onSetBase,
                    onCollectData = onCollectData,
                    onTransmitData = onTransmitData,
                    onHibernate = onHibernate,
                    onReset = onReset,
                    onStartMission = onStartMission,
                    onUpgradeBattery = onUpgradeBattery,
                    onUpgradeCargo = onUpgradeCargo,
                    onUpgradeCooling = onUpgradeCooling,
                    onDelete = onDelete,
                )
                DroneDetailUiState.NotFound -> NotFoundBox(onBack)
                is DroneDetailUiState.Error -> ErrorBox(state.canRetry, state.message, onRetry)
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
        CircularProgressIndicator(color = DroneColors.Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Установка сетевого соединения с дроном...",
            color = DroneColors.Cyan,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContentBox(
    drone: Drone,
    telemetry: DroneTelemetryDto,
    credits: Int,
    soundManager: SoundManager,
    onRename: (String) -> Unit,
    onSetBase: (String) -> Unit,
    onCollectData: () -> Unit,
    onTransmitData: () -> Unit,
    onHibernate: () -> Unit,
    onReset: () -> Unit,
    onStartMission: (Int) -> Unit,
    onUpgradeBattery: () -> Unit,
    onUpgradeCargo: () -> Unit,
    onUpgradeCooling: () -> Unit,
    onDelete: () -> Unit,
) {
    val style = drone.status.style()
    val currentBase = baseFor(drone.baseCity)
    val weatherDesc = weatherDescription(telemetry.weather.weatherCode)

    var isEditingName by remember { mutableStateOf(value = false) }
    var nameInput by remember(drone.name) { mutableStateOf(drone.name) }
    var showResetConfirmDialog by remember { mutableStateOf(value = false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(value = false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = style.cardColor),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DroneColors.CyanDark,
                ) {
                    Text(
                        text = drone.rankName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DroneColors.Cyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DroneColors.GoodDark,
                ) {
                    Text(
                        text = "Кредиты: $credits ⚡",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DroneColors.Good,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        label = { Text("Имя дрона", color = DroneColors.Cyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DroneColors.Cyan,
                            unfocusedBorderColor = DroneColors.SurfaceBorder,
                            focusedTextColor = DroneColors.TextPrimary,
                            unfocusedTextColor = DroneColors.TextPrimary,
                        ),
                    )
                    Button(
                        onClick = {
                            soundManager.play(SoundManager.Effect.CLICK)
                            onRename(nameInput)
                            isEditingName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
                        shape = RoundedCornerShape(10.dp),
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
                        color = DroneColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            soundManager.play(SoundManager.Effect.CLICK)
                            isEditingName = true
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Переименовать", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = style.badgeBg,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    StatusImage(
                        status = drone.status,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp),
                    )
                    Text(
                        text = style.text,
                        fontWeight = FontWeight.Bold,
                        color = style.accent,
                        fontSize = 14.sp,
                    )
                }
            }

            if (drone.status == Drone.Status.ON_MISSION) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Выполняется полёт: еще ${drone.activeMissionRemaining} сек",
                    color = DroneColors.Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { 1f - (drone.activeMissionRemaining.toFloat() / drone.activeMissionDuration) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .padding(top = 4.dp),
                    color = DroneColors.Cyan,
                    trackColor = DroneColors.SurfaceAlt,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailMetricBar("Заряд батареи", drone.battery, "% (макс ${drone.maxBattery}%)", DroneColors.Cyan)
            DetailMetricBar("Заполненность памяти", drone.cargo, "% (макс ${drone.maxCargo}%)", DroneColors.Pink)
            DetailMetricBar(
                "Температура ядра",
                drone.temperature,
                "°C (нагрев +${drone.heatRate}°C)",
                if (drone.temperature >= 80) DroneColors.Danger else DroneColors.Good,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Выбор базы дислокации дрона:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DroneColors.Cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(ALL_BASES) { b ->
                    val isSelected = b.city.equals(drone.baseCity, ignoreCase = true)
                    Button(
                        onClick = {
                            soundManager.play(SoundManager.Effect.CLICK)
                            onSetBase(b.city)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) DroneColors.Cyan else DroneColors.SurfaceAlt,
                            contentColor = if (isSelected) Color(0xFF0B0F14) else DroneColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(b.city, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DroneColors.SurfaceAlt),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Сетевая телеметрия:",
                            fontWeight = FontWeight.Bold,
                            color = DroneColors.Cyan,
                            fontSize = 13.sp,
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (telemetry.isOnlineData) DroneColors.GoodDark else DroneColors.WarningDark,
                        ) {
                            Text(
                                text = if (telemetry.isOnlineData) "🌐 Open-Meteo (Онлайн)" else "📡 Офлайн",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isOnlineData) DroneColors.Good else DroneColors.Warning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("База: ${currentBase.city} (${currentBase.latitude}, ${currentBase.longitude})", color = DroneColors.TextPrimary, fontSize = 12.sp)
                    Text("Погода: $weatherDesc, ветер ${telemetry.weather.windSpeed} м/с", color = DroneColors.TextPrimary, fontSize = 12.sp)
                    Text("Модель: ${telemetry.model}", color = DroneColors.TextPrimary, fontSize = 12.sp)
                    Text("Прошивка: ${telemetry.firmware}", color = DroneColors.TextPrimary, fontSize = 12.sp)
                    Text("Дальность: ${telemetry.rangeKm} км", color = DroneColors.TextPrimary, fontSize = 12.sp)
                    Text("Сенсоры: ${telemetry.sensorType}", color = DroneColors.TextPrimary, fontSize = 12.sp)
                    Text(telemetry.notes, color = DroneColors.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = "Разведывательные вылеты (заправка ⚡):",
        fontWeight = FontWeight.Bold,
        color = DroneColors.Cyan,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val cost15 = drone.missionLaunchCost(15)
        val cost30 = drone.missionLaunchCost(30)
        val cost60 = drone.missionLaunchCost(60)

        Button(
            onClick = {
                soundManager.play(SoundManager.Effect.SUCCESS)
                onStartMission(15)
            },
            enabled = drone.canStartMission(15, credits),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(6.dp),
        ) {
            Text("15с (-${cost15}⚡)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Button(
            onClick = {
                soundManager.play(SoundManager.Effect.SUCCESS)
                onStartMission(30)
            },
            enabled = drone.canStartMission(30, credits),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(6.dp),
        ) {
            Text("30с (-${cost30}⚡)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Button(
            onClick = {
                soundManager.play(SoundManager.Effect.SUCCESS)
                onStartMission(60)
            },
            enabled = drone.canStartMission(60, credits),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Pink, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(6.dp),
        ) {
            Text("60с (-${cost60}⚡)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = "Улучшения (Ранг ${drone.rankLevel}, цена ${drone.upgradeCost}⚡):",
        fontWeight = FontWeight.Bold,
        color = DroneColors.Pink,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedButton(
            onClick = {
                soundManager.play(SoundManager.Effect.SUCCESS)
                onUpgradeBattery()
            },
            enabled = credits >= drone.upgradeCost,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Text("+${drone.batteryBoostAmount}% Заряд", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = {
                soundManager.play(SoundManager.Effect.SUCCESS)
                onUpgradeCargo()
            },
            enabled = credits >= drone.upgradeCost,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Good),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Text("+${drone.cargoBoostAmount}% Память", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = {
                soundManager.play(SoundManager.Effect.SUCCESS)
                onUpgradeCooling()
            },
            enabled = (credits >= drone.upgradeCost) && (drone.heatRate > 5),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Pink),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Text("-${drone.coolingBoostAmount}°C Нагрев", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            soundManager.play(SoundManager.Effect.CLICK)
            onCollectData()
        },
        enabled = drone.canCollectData,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        Text("Собрать данные окружения", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = {
            soundManager.play(SoundManager.Effect.SUCCESS)
            onTransmitData()
        },
        enabled = drone.canTransmitData,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Pink, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        Text("Передать данные на базу", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = {
            soundManager.play(SoundManager.Effect.SUCCESS)
            onHibernate()
        },
        enabled = drone.canHibernate,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3), contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        Text("Гибернация (зарядка и охлаждение)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedButton(
        onClick = {
            soundManager.play(SoundManager.Effect.WARNING)
            showResetConfirmDialog = true
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(10.dp),
    ) {
        Text("Сброс до заводских настроек", fontWeight = FontWeight.SemiBold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = {
            soundManager.play(SoundManager.Effect.DELETE)
            showDeleteConfirmDialog = true
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DroneColors.Danger,
            containerColor = DroneColors.DangerDark,
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(10.dp),
    ) {
        Text("Списать (удалить дрон)", fontWeight = FontWeight.Bold)
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Подтверждение сброса", color = DroneColors.Cyan, fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите сбросить все параметры дрона '${drone.name}' до заводских настроек?", color = DroneColors.TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.play(SoundManager.Effect.WARNING)
                        onReset()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Сбросить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Muted),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Отмена")
                }
            },
            containerColor = DroneColors.Surface,
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Списание дрона", color = DroneColors.Danger, fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите списать и удалить дрон '${drone.name}' из флота?", color = DroneColors.TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.play(SoundManager.Effect.DELETE)
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Danger, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Списать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Muted),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Отмена")
                }
            },
            containerColor = DroneColors.Surface,
        )
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
            color = DroneColors.Danger,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Вернуться к списку", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorBox(canRetry: Boolean, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = DroneColors.Danger,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (canRetry) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color(0xFF0B0F14)),
                shape = RoundedCornerShape(10.dp),
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
            Text(label, fontSize = 13.sp, color = DroneColors.TextSecondary)
            Text("$value$unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (color == Color.Unspecified) DroneColors.Cyan else color,
            trackColor = DroneColors.SurfaceAlt,
        )
    }
}
