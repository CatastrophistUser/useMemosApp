package com.usememos.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.usememos.android.LocalAppContainer
import com.usememos.android.ui.feed.FeedRoute
import com.usememos.android.ui.feed.FeedViewModel
import com.usememos.android.ui.feed.FeedViewModelFactory
import com.usememos.android.ui.settings.SettingsRoute
import com.usememos.android.ui.settings.SettingsViewModel
import com.usememos.android.ui.settings.SettingsViewModelFactory

@Composable
fun MemosApp(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val container = LocalAppContainer.current

    NavHost(
        navController = navController,
        startDestination = "feed",
        modifier = modifier,
    ) {
        composable("feed") {
            val viewModel: FeedViewModel = viewModel(factory = FeedViewModelFactory(container))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            FeedRoute(
                state = state,
                onContentChanged = viewModel::onDraftChanged,
                onVisibilitySelected = viewModel::onVisibilityChanged,
                onTagSelected = viewModel::onTagSelected,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onDateSelected = viewModel::onDateSelected,
                onMonthChanged = viewModel::onMonthChanged,
                onSaveClicked = viewModel::saveMemo,
                onRetrySync = viewModel::syncNow,
                onOpenSettings = { navController.navigate("settings") },
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(container))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.saveSucceeded) {
                if (state.saveSucceeded) {
                    viewModel.clearSaveSucceeded()
                    if (backStackEntry?.destination?.route == "settings") {
                        navController.popBackStack()
                    }
                }
            }
            SettingsRoute(
                state = state,
                onBaseUrlChanged = viewModel::onBaseUrlChanged,
                onTokenChanged = viewModel::onTokenChanged,
                onSave = viewModel::save,
                onMessageShown = viewModel::clearInfoMessage,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
