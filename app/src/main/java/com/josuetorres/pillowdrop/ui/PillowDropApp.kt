package com.josuetorres.pillowdrop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josuetorres.pillowdrop.ui.screens.AppsSelectorScreen
import com.josuetorres.pillowdrop.ui.screens.PermissionsScreen
import com.josuetorres.pillowdrop.ui.screens.SpikeLabScreen
import com.josuetorres.pillowdrop.ui.screens.TimerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillowDropApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.bannerFeedback) {
        val msg = state.bannerFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    val missingPerms = 4 - state.permissions.grantedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PillowDrop",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Bedtime, contentDescription = null) },
                    label = { Text("Rutina") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Apps, contentDescription = null) },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (missingPerms > 0) {
                                    Badge { Text("$missingPerms") }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Security, contentDescription = null)
                        }
                    },
                    label = { Text("Permisos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Outlined.Science, contentDescription = null) },
                    label = { Text("Spike Lab") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TimerScreen(
                    state = state,
                    onStartMinutes = viewModel::startQuickTimerMinutes,
                    onStartSixtySecTest = viewModel::startSixtySecondLockTest,
                    onStartTargetClock = viewModel::startTargetClockTimer,
                    onSnooze30m = viewModel::snoozeThirtyMinutes,
                    onExtend5m = viewModel::extendFiveMinutes,
                    onCancelTimer = viewModel::cancelActiveTimer,
                    onUpdateSettings = viewModel::updateSettings
                )
                1 -> AppsSelectorScreen(
                    state = state,
                    onUpdateSettings = viewModel::updateSettings,
                    onTogglePackage = viewModel::toggleSelectedApp,
                    onReloadApps = viewModel::loadInstalledApps
                )
                2 -> PermissionsScreen(
                    state = state,
                    onRefreshPermissions = viewModel::refreshPermissions
                )
                3 -> SpikeLabScreen(
                    state = state,
                    onHangUpNow = viewModel::hangUpCallNow,
                    onStartSixtySecLockTest = viewModel::startSixtySecondLockTest,
                    onRunSpike2KillApps = viewModel::runSpike2KillAppsNow,
                    onClearLogs = viewModel::clearLogs
                )
            }
        }
    }
}
