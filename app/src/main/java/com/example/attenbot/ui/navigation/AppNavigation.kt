package com.example.attenbot.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.screens.HelpScreen
import com.example.attenbot.ui.screens.HistoryScreen
import com.example.attenbot.ui.screens.RtcTimeScreen
import com.example.attenbot.ui.screens.SyncScreen
import com.example.attenbot.ui.screens.TeacherControlsScreen
import com.example.attenbot.ui.screens.TroubleshootingScreen
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentRoute by rememberSaveable { mutableStateOf(AppDestination.ROUTE_SYNC) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val syncUiState by viewModel.syncUiState.collectAsState()
    val diagnosticsState by viewModel.diagnosticsState.collectAsState()
    val historyUiState by viewModel.historyUiState.collectAsState()

    val currentDestination = when (currentRoute) {
        AppDestination.ROUTE_TEACHER -> AppDestination.Teacher
        AppDestination.ROUTE_RTC -> AppDestination.RtcTime
        AppDestination.ROUTE_TROUBLESHOOTING -> AppDestination.Troubleshooting
        AppDestination.ROUTE_HELP -> AppDestination.Help
        AppDestination.ROUTE_HISTORY -> AppDestination.History
        else -> AppDestination.Sync
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AppleTheme.colors.groupedBackground,
                drawerContentColor = AppleTheme.colors.textPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "AttenBot Menu",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTheme.colors.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text("Live Sync Dashboard", fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == AppDestination.ROUTE_SYNC,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentRoute = AppDestination.ROUTE_SYNC
                        },
                        icon = { Icon(AppDestination.Sync.icon, contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = AppleTheme.colors.accent.copy(alpha = 0.15f),
                            selectedTextColor = AppleTheme.colors.accent,
                            selectedIconColor = AppleTheme.colors.accent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AppDestination.drawerEntries.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.title) },
                            selected = currentRoute == destination.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                currentRoute = destination.route
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = AppleTheme.colors.accent.copy(alpha = 0.15f),
                                selectedTextColor = AppleTheme.colors.accent,
                                selectedIconColor = AppleTheme.colors.accent
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = AppleTheme.colors.pageBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppleTheme.colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = AppleTheme.colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppleTheme.colors.groupedBackground
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentDestination,
                    modifier = Modifier.fillMaxSize(),
                    label = "ScreenTransition"
                ) { destination ->
                    when (destination) {
                        AppDestination.Sync -> {
                            SyncScreen(
                                uiState = syncUiState,
                                onUrlChange = viewModel::updateEsp32Url,
                                onSaveUrlClick = viewModel::saveBaseUrl,
                                onConnectClick = viewModel::connectToDevice,
                                onTestConnectionClick = viewModel::testConnection,
                                onRefreshStatusClick = viewModel::refreshStatus,
                                onRefreshAttendanceClick = viewModel::refreshAttendance,
                                onDownloadCsvClick = { pin, uri ->
                                    viewModel.downloadCsv(pin, uri, context.contentResolver)
                                },
                                onResetAttendanceClick = viewModel::clearAttendanceForNextLecture,
                                onNavigateToTeacherControls = { currentRoute = AppDestination.ROUTE_TEACHER },
                                onNavigateToRtc = { currentRoute = AppDestination.ROUTE_RTC }
                            )
                        }
                        AppDestination.Teacher -> {
                            TeacherControlsScreen(
                                uiState = syncUiState,
                                onDownloadCsvClick = { pin, uri ->
                                    viewModel.downloadCsv(pin, uri, context.contentResolver)
                                },
                                onResetAttendanceClick = viewModel::clearAttendanceForNextLecture,
                                onNavigateToRtc = { currentRoute = AppDestination.ROUTE_RTC },
                                onClearMessages = viewModel::clearMessages
                            )
                        }
                        AppDestination.RtcTime -> {
                            RtcTimeScreen(
                                uiState = syncUiState,
                                onSetRtcTimeClick = viewModel::setRtcTime
                            )
                        }
                        AppDestination.Troubleshooting -> {
                            TroubleshootingScreen(
                                syncState = syncUiState,
                                diagnosticsState = diagnosticsState,
                                onTestStatusEndpoint = viewModel::refreshStatus,
                                onTestAttendanceEndpoint = viewModel::refreshAttendance
                            )
                        }
                        AppDestination.Help -> {
                            HelpScreen(
                                uiState = syncUiState,
                                onUrlChange = viewModel::updateEsp32Url,
                                onSaveUrlClick = viewModel::saveBaseUrl
                            )
                        }
                        AppDestination.History -> {
                            HistoryScreen(
                                uiState = historyUiState,
                                onSearchQueryChange = viewModel::onSearchQueryChange,
                                onYearFilterChange = viewModel::onYearFilterChange
                            )
                        }
                    }
                }
            }
        }
    }
}
