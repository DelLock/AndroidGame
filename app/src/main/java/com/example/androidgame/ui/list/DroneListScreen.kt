package com.example.androidgame.ui.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidgame.domain.model.Drone
import com.example.androidgame.domain.model.startDrones
import com.example.androidgame.ui.components.DroneColors
import com.example.androidgame.ui.components.DroneImage
import com.example.androidgame.ui.components.StatusImage
import com.example.androidgame.ui.components.style
import com.example.androidgame.util.ConnectivityMonitor
import com.example.androidgame.util.SoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneListScreen(
    state: DroneListUiState,
    onAddDrone: (String?) -> Unit,
    onDeleteDrone: (Int) -> Unit,
    onSelectDrone: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterChange: (DroneFilter) -> Unit = {},
    onRestoreDeletedDrone: () -> Unit = {},
    onDismissMissionNotice: () -> Unit = {},
    onHibernateAll: () -> Unit = {},
    onTransmitAll: () -> Unit = {},
) {
    val context = LocalContext.current
    val monitor = remember(context) { ConnectivityMonitor(context) }
    DisposableEffect(monitor) {
        onDispose {
            monitor.unregister()
        }
    }
    val isOnline by monitor.online.collectAsState()
    val soundManager = remember { SoundManager() }
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var filterMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var newDroneNameInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "DroneHub",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = DroneColors.TextPrimary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DroneColors.GoodDark,
                            ) {
                                Text(
                                    text = "${state.credits} ⚡",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DroneColors.Good,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isOnline) DroneColors.GoodDark else DroneColors.DangerDark,
                            ) {
                                Text(
                                    text = if (isOnline) "🌐 Онлайн" else "📡 Офлайн",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) DroneColors.Good else DroneColors.Danger,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            OutlinedButton(
                                onClick = { soundManager.enabled = !soundManager.enabled },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DroneColors.Cyan,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(if (soundManager.enabled) "🔊" else "🔇", fontSize = 12.sp)
                            }
                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск дрона по названию...", color = DroneColors.Muted) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DroneColors.Cyan,
                    unfocusedBorderColor = DroneColors.SurfaceBorder,
                    focusedContainerColor = DroneColors.Surface,
                    unfocusedContainerColor = DroneColors.Surface,
                    focusedTextColor = DroneColors.TextPrimary,
                    unfocusedTextColor = DroneColors.TextPrimary,
                ),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        soundManager.play(SoundManager.Effect.CLICK)
                        newDroneNameInput = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DroneColors.Cyan,
                        contentColor = Color(0xFF0B0F14),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text("+ Дрон", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Box {
                    val filterLabel = when (state.selectedFilter) {
                        DroneFilter.ALL -> "Все"
                        DroneFilter.NORMAL -> "Норма"
                        DroneFilter.ON_MISSION -> "Миссия"
                        DroneFilter.OVERHEATED -> "Перегрев"
                        DroneFilter.DEPLETED -> "Разряд"
                        DroneFilter.FULL_CARGO -> "Память"
                    }

                    OutlinedButton(
                        onClick = {
                            soundManager.play(SoundManager.Effect.CLICK)
                            filterMenuExpanded = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DroneColors.Cyan,
                            containerColor = DroneColors.Surface,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text("Фильтр: $filterLabel ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false },
                        containerColor = DroneColors.SurfaceAlt,
                    ) {
                        val filtersList = listOf(
                            DroneFilter.ALL to "Все дроны",
                            DroneFilter.NORMAL to "Системы в норме",
                            DroneFilter.ON_MISSION to "На миссии",
                            DroneFilter.OVERHEATED to "Критический перегрев",
                            DroneFilter.DEPLETED to "Низкий заряд",
                            DroneFilter.FULL_CARGO to "Хранилище переполнено",
                        )
                        filtersList.forEach { (filter, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (state.selectedFilter == filter) DroneColors.Cyan else DroneColors.TextPrimary,
                                        fontWeight = if (state.selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                    )
                                },
                                onClick = {
                                    soundManager.play(SoundManager.Effect.CLICK)
                                    onFilterChange(filter)
                                    filterMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        soundManager.play(SoundManager.Effect.SUCCESS)
                        onHibernateAll()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DroneColors.Cyan,
                        containerColor = DroneColors.Surface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Text("Зарядить все", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        soundManager.play(SoundManager.Effect.SUCCESS)
                        onTransmitAll()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DroneColors.Pink,
                        containerColor = DroneColors.Surface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Text("Передать все", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(
                visible = state.missionNoticeMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut(animationSpec = tween(durationMillis = 500)) + shrinkVertically(),
            ) {
                AnimatedContent(
                    targetState = state.missionNoticeMessage,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "MissionNoticeContent",
                ) { currentMessage ->
                    if (currentMessage != null) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DroneColors.CyanDark),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = currentMessage,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DroneColors.Cyan,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Button(
                                        onClick = {
                                            soundManager.play(SoundManager.Effect.CLICK)
                                            onDismissMissionNotice()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DroneColors.Cyan,
                                            contentColor = Color(0xFF0B0F14),
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.padding(start = 8.dp),
                                    ) {
                                        Text("ОК", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = (state.lastDeletedDroneName != null) && (state.undoTimerSeconds > 0),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut(animationSpec = tween(durationMillis = 500)) + shrinkVertically(),
            ) {
                if (state.lastDeletedDroneName != null) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DroneColors.SurfaceAlt),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Списан: ${state.lastDeletedDroneName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DroneColors.TextPrimary,
                                    )
                                    Text(
                                        text = "Уведомление исчезнет через ${state.undoTimerSeconds} сек",
                                        fontSize = 11.sp,
                                        color = DroneColors.Muted,
                                    )
                                }
                                Button(
                                    onClick = {
                                        soundManager.play(SoundManager.Effect.SUCCESS)
                                        onRestoreDeletedDrone()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DroneColors.Cyan,
                                        contentColor = Color(0xFF0B0F14),
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Вернуть", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    Text(
                        text = "Укажите название дрона или оставьте пустым для названия по умолчанию:",
                        fontSize = 13.sp,
                        color = DroneColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDroneNameInput,
                        onValueChange = { newDroneNameInput = it },
                        singleLine = true,
                        label = { Text("Название дрона", color = DroneColors.Cyan) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DroneColors.Cyan,
                            unfocusedBorderColor = DroneColors.SurfaceBorder,
                            focusedTextColor = DroneColors.TextPrimary,
                            unfocusedTextColor = DroneColors.TextPrimary,
                        ),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DroneColors.Cyan,
                        contentColor = Color(0xFF0B0F14),
                    ),
                    shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(16.dp),
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
                    .padding(end = 10.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = drone.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DroneColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DroneColors.CyanDark,
                        modifier = Modifier.padding(start = 6.dp),
                    ) {
                        Text(
                            text = "Ранг ${drone.rankLevel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = DroneColors.Cyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = style.badgeBg,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        StatusImage(
                            status = drone.status,
                            modifier = Modifier
                                .size(12.dp)
                                .padding(end = 4.dp),
                        )
                        Text(
                            text = if (drone.status == Drone.Status.ON_MISSION) "Миссия: ${drone.activeMissionRemaining}с" else style.text,
                            fontWeight = FontWeight.Bold,
                            color = style.accent,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⚡ ${drone.battery}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DroneColors.Cyan,
                    )
                    Text(
                        text = "💾 ${drone.cargo}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DroneColors.Pink,
                    )
                    Text(
                        text = "🌡️ ${drone.temperature}°C",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (drone.temperature >= 80) DroneColors.Danger else DroneColors.Good,
                    )
                }
            }

            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DroneColors.Danger,
                    containerColor = DroneColors.DangerDark,
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("Удалить", fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
