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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var showAddDialog by remember { mutableStateOf(value = false) }
    var newDroneNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DroneHub", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
        ) {
            Button(
                onClick = {
                    newDroneNameInput = ""
                    showAddDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
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
                        color = Color(0xFF8B949E),
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
                            onSelect = { onSelectDrone(drone.id) },
                            onDelete = { onDeleteDrone(drone.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Создание дрона", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Укажите название дрона или оставьте пустым для применения имени по умолчанию:", fontSize = 13.sp, color = Color.White)
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
                        onAddDrone(newDroneNameInput.ifBlank { null })
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                ) {
                    Text("Создать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E)),
                ) {
                    Text("Отмена")
                }
            },
            containerColor = Color(0xFF161B22),
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
    val (cardColor, statusText, statusColor) = when (drone.status) {
        Drone.Status.NORMAL -> Triple(Color(0xFF161B22), "Системы в норме", Color(0xFF00E5FF))
        Drone.Status.OVERHEATED -> Triple(Color(0xFF2D1214), "КРИТИЧЕСКИЙ ПЕРЕГРЕВ", Color(0xFFFF1744))
        Drone.Status.DEPLETED -> Triple(Color(0xFF2D2012), "НИЗКИЙ ЗАРЯД", Color(0xFFFF9100))
        Drone.Status.FULL_CARGO -> Triple(Color(0xFF122D1A), "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО", Color(0xFF00E676))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
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
                        text = statusText,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Заряд: ${drone.battery}% | Память: ${drone.cargo}% | Темп: ${drone.temperature}°C",
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E),
                )
            }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF1744),
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
