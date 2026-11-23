package com.example.androidgame.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.ui.detail.DroneDetailScreen
import com.example.androidgame.ui.detail.DroneDetailViewModel
import com.example.androidgame.ui.list.DroneListScreen
import com.example.androidgame.ui.list.DroneListViewModel
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
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onFilterChange = viewModel::onFilterChange,
                    onAddDrone = viewModel::onAddDrone,
                    onDeleteDrone = viewModel::onDeleteDrone,
                    onRestoreDeletedDrone = viewModel::onRestoreDeletedDrone,
                    onDismissMissionNotice = viewModel::onDismissMissionNotice,
                    onHibernateAll = viewModel::onHibernateAll,
                    onTransmitAll = viewModel::onTransmitAll,
                    onSelectDrone = { id ->
                        if (backStack.lastOrNull() == DroneListKey) {
                            backStack.add(DroneDetailKey(id))
                        }
                    },
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
                    onRetry = viewModel::onRetry,
                    onRename = viewModel::onRename,
                    onSetBase = viewModel::onSetBase,
                    onCollectData = viewModel::onCollectData,
                    onTransmitData = viewModel::onTransmitData,
                    onHibernate = viewModel::onHibernate,
                    onReset = viewModel::onReset,
                    onStartMission = viewModel::onStartMission,
                    onUpgradeBattery = viewModel::onUpgradeBattery,
                    onUpgradeCargo = viewModel::onUpgradeCargo,
                    onUpgradeCooling = viewModel::onUpgradeCooling,
                    onDelete = {
                        viewModel.onDelete()
                        close()
                    },
                    onBack = close,
                )
            }
        },
    )
}
