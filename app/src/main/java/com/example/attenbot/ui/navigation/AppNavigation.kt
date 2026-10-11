package com.example.attenbot.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.attenbot.ui.screens.ExportScreen
import com.example.attenbot.ui.screens.HistoryScreen
import com.example.attenbot.ui.screens.SyncScreen
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.viewmodel.AttendanceViewModel

@Composable
fun AppNavigation(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val adaptiveInfo = currentWindowAdaptiveInfo()
    @Suppress("DEPRECATION")
    val isWideScreen = (adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) ||
            (adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM)

    var selectedTabRoute by rememberSaveable { mutableStateOf(AppDestination.Sync.route) }

    val syncUiState by viewModel.syncUiState.collectAsState()
    val historyUiState by viewModel.historyUiState.collectAsState()
    val exportUiState by viewModel.exportUiState.collectAsState()

    val currentDestination = AppDestination.entries.find { it.route == selectedTabRoute } ?: AppDestination.Sync

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppleTheme.colors.pageBackground,
        bottomBar = {
            if (!isWideScreen) {
                NavigationBar(
                    containerColor = AppleTheme.colors.groupedBackground,
                    contentColor = AppleTheme.colors.textPrimary
                ) {
                    AppDestination.entries.forEach { destination ->
                        val isSelected = destination.route == selectedTabRoute
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabRoute = destination.route },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = 12.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleTheme.colors.accent,
                                selectedTextColor = AppleTheme.colors.accent,
                                unselectedIconColor = AppleTheme.colors.textSecondary,
                                unselectedTextColor = AppleTheme.colors.textSecondary,
                                indicatorColor = AppleTheme.colors.accent.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = AppleTheme.colors.groupedBackground,
                    contentColor = AppleTheme.colors.textPrimary
                ) {
                    AppDestination.entries.forEach { destination ->
                        val isSelected = destination.route == selectedTabRoute
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { selectedTabRoute = destination.route },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = 12.sp
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AppleTheme.colors.accent,
                                selectedTextColor = AppleTheme.colors.accent,
                                unselectedIconColor = AppleTheme.colors.textSecondary,
                                unselectedTextColor = AppleTheme.colors.textSecondary,
                                indicatorColor = AppleTheme.colors.accent.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }

            Crossfade(
                targetState = currentDestination,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = "ScreenTransition"
            ) { destination ->
                when (destination) {
                    AppDestination.Sync -> {
                        SyncScreen(
                            uiState = syncUiState,
                            onUrlChange = viewModel::updateEsp32Url,
                            onSyncClick = { viewModel.syncWithEsp32() },
                            onNavigateToHistory = { selectedTabRoute = AppDestination.History.route },
                            onNavigateToExport = { selectedTabRoute = AppDestination.Export.route }
                        )
                    }
                    AppDestination.History -> {
                        HistoryScreen(
                            uiState = historyUiState,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onYearFilterChange = viewModel::onYearFilterChange
                        )
                    }
                    AppDestination.Export -> {
                        ExportScreen(
                            uiState = exportUiState,
                            onExportClick = { viewModel.exportMasterCsv(context) }
                        )
                    }
                }
            }
        }
    }
}
