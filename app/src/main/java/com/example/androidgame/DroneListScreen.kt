package com.example.androidgame

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneListScreen(
    state: DroneListUiState,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterChange: (DroneFilter) -> Unit = {},
    onAddDrone: (String?) -> Unit,
    onDeleteDrone: (Int) -> Unit,
    onRestoreDeletedDrone: () -> Unit = {},
    onHibernateAll: () -> Unit = {},
    onTransmitAll: () -> Unit = {},
    onSelectDrone: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val monitor = remember(context) { ConnectivityMonitor(context) }
    val isOnline by monitor.online.collectAsState()
    val soundManager = remember { SoundManager() }

    var showAddDialog by remember { mutableStateOf(value = false) }
    var newDroneNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("DroneHub", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Кредиты: ${state.credits}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DroneColors.Good,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = if (isOnline) "Онлайн" else "Офлайн",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) DroneColors.Good else DroneColors.Danger,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            OutlinedButton(
                                onClick = { soundManager.enabled = !soundManager.enabled },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
                            ) {
                                Text(if (soundManager.enabled) "Звук" else "Без звука", fontSize = 10.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DroneColors.Surface,
                    titleContentColor = DroneColors.Cyan,
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
                .padding(16.dp),
        ) {
            // Поисковая строка и создание дрона
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск дрона по названию", color = DroneColors.Cyan) },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Панель фильтрации по статусу
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val filters = listOf(
                    DroneFilter.ALL to "Все",
                    DroneFilter.NORMAL to "Норма",
                    DroneFilter.ON_MISSION to "Миссия",
                    DroneFilter.OVERHEATED to "Перегрев",
                    DroneFilter.DEPLETED to "Разряд",
                    DroneFilter.FULL_CARGO to "Память",
                )
                items(filters) { (filter, label) ->
                    val isSelected = state.selectedFilter == filter
                    Button(
                        onClick = {
                            soundManager.play(SoundManager.Effect.CLICK)
                            onFilterChange(filter)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) DroneColors.Cyan else DroneColors.Surface,
                            contentColor = if (isSelected) Color.Black else Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Панель массовых действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        soundManager.play(SoundManager.Effect.CLICK)
                        newDroneNameInput = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color.Black),
                ) {
                    Text("+ Дрон", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        soundManager.play(SoundManager.Effect.SUCCESS)
                        onHibernateAll()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
                ) {
                    Text("Зарядить всех", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        soundManager.play(SoundManager.Effect.SUCCESS)
                        onTransmitAll()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Pink),
                ) {
                    Text("Передать всё", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Уведомление о восстановлении списанного дрона
            if (state.lastDeletedDroneName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DroneColors.SurfaceAlt),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Списан: ${state.lastDeletedDroneName}",
                            fontSize = 12.sp,
                            color = Color.White,
                        )
                        OutlinedButton(
                            onClick = {
                                soundManager.play(SoundManager.Effect.SUCCESS)
                                onRestoreDeletedDrone()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Cyan),
                        ) {
                            Text("Вернуть", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.drones.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Список дронов пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DroneColors.Muted,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.drones,
                        key = { it.id },
                    ) { drone ->
                        DroneItemCard(
                            drone = drone,
                            onSelect = {
                                soundManager.play(SoundManager.Effect.CLICK)
                                onSelectDrone(drone.id)
                            },
                            onDelete = {
                                soundManager.play(SoundManager.Effect.DELETE)
                                onDeleteDrone(drone.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Создание дрона", color = DroneColors.Cyan, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Укажите название дрона или оставьте пустым для названия по умолчанию:", fontSize = 13.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDroneNameInput,
                        onValueChange = { newDroneNameInput = it },
                        singleLine = true,
                        label = { Text("Название дрона") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.play(SoundManager.Effect.SUCCESS)
                        onAddDrone(newDroneNameInput.ifBlank { null })
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DroneColors.Cyan, contentColor = Color.Black),
                ) {
                    Text("Создать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        soundManager.play(SoundManager.Effect.CLICK)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DroneColors.Muted),
                ) {
                    Text("Отмена")
                }
            },
            containerColor = DroneColors.Surface,
        )
    }
}

@Composable
fun DroneItemCard(
    drone: Drone,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = drone.status.style()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = style.cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DroneImage(
                status = drone.status,
                modifier = Modifier
                    .size(52.dp)
                    .padding(end = 8.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = drone.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                    Text(
                        text = " [${drone.rankName}]",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = DroneColors.Cyan,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusImage(
                        status = drone.status,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp),
                    )
                    Text(
                        text = if (drone.status == Drone.Status.ON_MISSION) "Миссия: еще ${drone.activeMissionRemaining} с" else style.text,
                        fontWeight = FontWeight.SemiBold,
                        color = style.accent,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Заряд: ${drone.battery}% / ${drone.maxBattery}% | Память: ${drone.cargo}% | Темп: ${drone.temperature}°C",
                    fontSize = 11.sp,
                    color = DroneColors.Muted,
                )
            }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DroneColors.Danger,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("Удалить", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DroneListScreenPreview() {
    MaterialTheme {
        DroneListScreen(
            state = DroneListUiState(drones = startDrones()),
            onAddDrone = {},
            onDeleteDrone = {},
            onSelectDrone = {},
        )
    }
}
