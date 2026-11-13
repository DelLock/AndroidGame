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
    onAddDrone: (String?) -> Unit,
    onDeleteDrone: (Int) -> Unit,
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
                                text = if (isOnline) "Онлайн" else "Офлайн",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) DroneColors.Good else DroneColors.Danger,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            OutlinedButton(
                                onClick = {
                                    soundManager.enabled = !soundManager.enabled
                                },
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
            Button(
                onClick = {
                    soundManager.play(SoundManager.Effect.CLICK)
                    newDroneNameInput = ""
                    showAddDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DroneColors.Cyan,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Добавить дрон", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                Text(
                    text = drone.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusImage(
                        status = drone.status,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp),
                    )
                    Text(
                        text = style.text,
                        fontWeight = FontWeight.SemiBold,
                        color = style.accent,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Заряд: ${drone.battery}% | Память: ${drone.cargo}% | Темп: ${drone.temperature}°C",
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
