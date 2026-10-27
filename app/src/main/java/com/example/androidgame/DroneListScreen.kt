package com.example.androidgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
fun DroneListScreen(
    state: DroneListUiState,
    onAddDrone: () -> Unit,
    onDeleteDrone: (Int) -> Unit,
    onSelectDrone: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Флот дронов-разведчиков") },
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
        ) {
            Button(
                onClick = onAddDrone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Добавить дрон (исходное состояние)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.drones.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Список дронов пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.drones,
                        key = { it.id }
                    ) { drone ->
                        DroneItemCard(
                            drone = drone,
                            onSelect = { onSelectDrone(drone.id) },
                            onDelete = { onDeleteDrone(drone.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DroneItemCard(
    drone: Drone,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (cardColor, statusText, statusColor) = when (drone.status) {
        Drone.Status.NORMAL -> Triple(MaterialTheme.colorScheme.surfaceVariant, "Системы в норме", MaterialTheme.colorScheme.onSurfaceVariant)
        Drone.Status.OVERHEATED -> Triple(Color(0xFFFFCDD2), "КРИТИЧЕСКИЙ ПЕРЕГРЕВ", Color.Red)
        Drone.Status.DEPLETED -> Triple(Color(0xFFFFF9C4), "НИЗКИЙ ЗАРЯД", Color(0xFFE65100))
        Drone.Status.FULL_CARGO -> Triple(Color(0xFFC8E6C9), "ХРАНИЛИЩЕ ПЕРЕПОЛНЕНО", Color(0xFF2E7D32))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drone.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Заряд: ${drone.battery}% | Память: ${drone.cargo}% | Темп: ${drone.temperature}°C",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Text("Удалить", color = Color.White, fontSize = 12.sp)
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
            onSelectDrone = {}
        )
    }
}
