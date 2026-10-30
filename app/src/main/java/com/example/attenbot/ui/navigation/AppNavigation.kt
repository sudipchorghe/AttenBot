package com.example.attenbot.ui.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.attenbot.ui.screens.FocusApp
import com.example.attenbot.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.delay

/** Only two pages. Old screen files may stay in the project; they are no longer routed. */
@Composable
fun AppNavigation(viewModel: AttendanceViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("attenbot_focus", Context.MODE_PRIVATE) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var className by remember { mutableStateOf(preferences.getString("class", "TE5K-B") ?: "TE5K-B") }
    var room by remember { mutableStateOf(preferences.getString("room", "") ?: "") }
    var total by remember { mutableIntStateOf(preferences.getInt("total", 64).coerceAtLeast(1)) }
    var autoSync by remember { mutableStateOf(preferences.getBoolean("auto_sync", true)) }
    val state by viewModel.syncUiState.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnosticsState.collectAsStateWithLifecycle()
    val history by viewModel.historyUiState.collectAsStateWithLifecycle()
    val export by viewModel.exportUiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(state)
    // Never use the legacy /download endpoint for polling: it can close attendance.
    // Wait for both requests to finish before the next interval and pause in background.
    LaunchedEffect(lifecycleOwner, autoSync, state.baseUrl) {
        if (autoSync) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val current = latest
                if (!current.isConnecting && !current.isRefreshing && !current.isDownloading &&
                    !current.isResetting && !current.isSettingRtcTime) {
                    viewModel.refreshAll()
                }
                delay(15_000)
            }
        }
    }
    BackHandler(settings) { settings = false }
    FocusApp(
        state = state, diagnostics = diagnostics, history = history, export = export,
        settings = settings, onSettings = { settings = it },
        className = className, room = room, total = total, autoSync = autoSync,
        onProfile = { newClass, newRoom, newTotal ->
            className = newClass; room = newRoom; total = newTotal
            preferences.edit().putString("class", newClass).putString("room", newRoom)
                .putInt("total", newTotal).apply()
        },
        onAutoSync = { autoSync = it; preferences.edit().putBoolean("auto_sync", it).apply() },
        onSaveUrl = viewModel::saveBaseUrl,
        onRefresh = viewModel::refreshAll,
        onDownload = { pin, uri -> viewModel.downloadCsv(pin, uri, context.contentResolver) },
        onReset = viewModel::clearAttendanceForNextLecture,
        onClock = viewModel::setRtcTime,
        onSearch = viewModel::onSearchQueryChange,
        onExportHistory = { uri -> viewModel.exportMasterCsv(context, uri) },
        onClearMessages = viewModel::clearMessages,
        modifier = modifier
    )
}