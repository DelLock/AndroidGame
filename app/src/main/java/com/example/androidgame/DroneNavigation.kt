package com.example.androidgame

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
data object DroneListKey : NavKey

@Serializable
data class DroneDetailKey(val droneId: Int) : NavKey

@Composable
fun DroneNavigation(repo: DroneRepository, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(DroneListKey)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<DroneListKey> {
                val viewModel = viewModel { DroneListViewModel(repo) }
                DroneListScreen(
                    state = viewModel.uiState,
                    onAddDrone = viewModel::onAddDrone,
                    onDeleteDrone = viewModel::onDeleteDrone,
                    onSelectDrone = { id ->
                        if (backStack.lastOrNull() == DroneListKey) {
                            backStack.add(DroneDetailKey(id))
                        }
                    }
                )
            }
            entry<DroneDetailKey> { key ->
                val viewModel = viewModel { DroneDetailViewModel(key.droneId, repo) }
                val close: () -> Unit = {
                    if (backStack.lastOrNull() == key) {
                        backStack.removeLastOrNull()
                    }
                }
                DroneDetailScreen(
                    state = viewModel.uiState,
                    onCollectData = viewModel::onCollectData,
                    onTransmitData = viewModel::onTransmitData,
                    onHibernate = viewModel::onHibernate,
                    onReset = viewModel::onReset,
                    onDelete = {
                        viewModel.onDelete()
                        close()
                    },
                    onBack = close
                )
            }
        }
    )
}
