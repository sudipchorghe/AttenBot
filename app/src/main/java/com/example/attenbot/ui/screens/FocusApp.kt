package com.example.attenbot.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.state.DiagnosticsState
import com.example.attenbot.ui.state.ExportUiState
import com.example.attenbot.ui.state.HistoryUiState
import com.example.attenbot.ui.state.SyncUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlin.math.roundToInt

private val Subtitle = Color(0xFFA4B6AA)
private val Pill = Color(0xFF253E2E)

@Composable
fun FocusApp(
    state: SyncUiState,
    diagnostics: DiagnosticsState,
    history: HistoryUiState,
    export: ExportUiState,
    settings: Boolean,
    onSettings: (Boolean) -> Unit,
    className: String,
    room: String,
    total: Int,
    autoSync: Boolean,
    onProfile: (String, String, Int) -> Unit,
    onAutoSync: (Boolean) -> Unit,
    onSaveUrl: (String) -> Unit,
    onRefresh: () -> Unit,
    onDownload: (String, Uri) -> Unit,
    onReset: (String) -> Unit,
    onClock: (String, String) -> Unit,
    onSearch: (String) -> Unit,
    onExportHistory: (Uri) -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf("") }
    var showStudents by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var pendingDownloadPin by remember { mutableStateOf("") }
    var dateTime by remember {
        mutableStateOf(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        )
    }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val openAction = { type: String ->
        onClearMessages()
        action = type
        pin = ""
        dialogError = null
        if (type == "clock") {
            dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
    }

    val busy = state.isConnecting || state.isRefreshing || state.isDownloading || state.isResetting || state.isSettingRtcTime
    val verified = state.isConnected && state.lastConnectionTime != null

    val download = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { onDownload(pendingDownloadPin, it) }
    }

    val historyDownload = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { onExportHistory(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (settings) "Settings & Tools" else "Attendance Focus",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (room.isBlank()) className else "$className · $room",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onSettings(!settings) }) {
                Icon(
                    imageVector = if (settings) Icons.Rounded.Close else Icons.Rounded.Settings,
                    contentDescription = if (settings) "Close Settings" else "Settings"
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!settings) {
                item {
                    FocusCard {
                        Text(
                            if (room.isBlank()) className else "$className · $room",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val countSize = if (maxWidth < 300.dp) 62.sp else 88.sp
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(if (verified) state.count.toString() else "—", fontSize = countSize, fontWeight = FontWeight.ExtraBold, letterSpacing = (-3).sp)
                                Text(" / $total", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                            }
                        }
                        Text("students present this session", color = Subtitle)
                        Spacer(Modifier.height(22.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill(if (verified) "${(total - state.count).coerceAtLeast(0)} not marked" else "— not marked", Modifier.weight(1f))
                            StatPill(if (verified) "${(state.count * 100f / total).roundToInt()}% present" else "—% present", Modifier.weight(1f))
                        }
                        if (state.count > total) Text("Device count exceeds class size. Update the total in Settings.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    FocusCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                            Text(when {
                                state.isConnecting || state.isRefreshing -> "Updating…"
                                state.lastConnectionTime == null -> "Not connected yet"
                                state.isConnected -> "Connected"
                                else -> "Disconnected"
                            }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Last synced: ${state.lastSyncTime ?: "Never"}", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(if (autoSync) "Auto-sync ON · every 15 seconds while open" else "Auto-sync OFF · enable in Settings", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Button(
                            onClick = onRefresh,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (state.isConnecting || state.isRefreshing) "Syncing…" else "Sync Now",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item {
                    Button(onClick = { openAction("download") }, enabled = !busy,
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 68.dp), shape = RoundedCornerShape(22.dp)) {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(Modifier.width(10.dp))
                        Text(if (state.isDownloading) "Downloading…" else "Download Attendance", fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    ExpandSection("Current attendance", "${state.liveRecords.size} records from the device", showStudents, { showStudents = !showStudents }) {
                        if (state.liveRecords.isEmpty()) Text("No records received yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else Text("Expand to review students in this session.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (showStudents) {
                    items(state.liveRecords) { student ->
                        FocusCard {
                            Text("${student.rollNumber} · ${student.name}", Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                            Text(student.timestamp, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item { Text("Your classroom, device and attendance tools", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    ExpandSection("Classroom", "Class name, room and student total", expanded == "Classroom", { expanded = if (expanded == "Classroom") "" else "Classroom" }) {
                        var nameDraft by remember(className) { mutableStateOf(className) }
                        var roomDraft by remember(room) { mutableStateOf(room) }
                        var totalDraft by remember(total) { mutableStateOf(total.toString()) }
                        Field(nameDraft, { nameDraft = it }, "Class name")
                        Field(roomDraft, { roomDraft = it }, "Room number")
                        OutlinedTextField(totalDraft, { totalDraft = it.filter(Char::isDigit).take(4) }, label = { Text("Total students") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { onProfile(nameDraft.trim(), roomDraft.trim(), totalDraft.toInt()); expanded = "" }, enabled = nameDraft.isNotBlank() && (totalDraft.toIntOrNull() ?: 0) > 0) { Text("Save classroom") }
                        Text("These are local display settings; they do not change the ESP32 roster.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    ExpandSection("Connection", "Device address and automatic sync", expanded == "Connection", { expanded = if (expanded == "Connection") "" else "Connection" }) {
                        var url by remember(state.esp32Url) { mutableStateOf(state.esp32Url) }
                        Field(url, { url = it }, "ESP32 URL")
                        Button(onClick = { onSaveUrl(url.trim()); onRefresh() }, enabled = url.isNotBlank() && !busy) { Text("Save & connect") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-sync", fontWeight = FontWeight.SemiBold)
                                Text("Refresh every 15 seconds while open", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(autoSync, onCheckedChange = onAutoSync)
                        }
                        Text("Connect your phone to the Attendance_Bot Wi-Fi network first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    ExpandSection("Teacher controls", "Start the next lecture and set the clock", expanded == "Teacher controls", { expanded = if (expanded == "Teacher controls") "" else "Teacher controls" }) {
                        OutlinedButton(onClick = { openAction("clock") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Set ESP32 clock") }
                        OutlinedButton(onClick = { openAction("reset") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear attendance / next lecture") }
                        Text("Teacher PIN required. Download the current session before clearing it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    ExpandSection("Attendance history", "Search saved records and export", expanded == "Attendance history", { expanded = if (expanded == "Attendance history") "" else "Attendance history" }) {
                        Field(history.searchQuery, onSearch, "Search name or roll number")
                        Text("${history.records.size} matching records")
                        Button(onClick = { historyDownload.launch("AttenBot_History_${LocalDate.now()}.csv") }, enabled = !export.isExporting) { Text(if (export.isExporting) "Exporting…" else "Export all saved history") }
                        export.exportSuccessMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        export.exportErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (history.records.isEmpty()) Text("No saved records match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (expanded == "Attendance history") {
                    items(history.records) { record ->
                        FocusCard {
                            Text("${record.rollNumber} · ${record.studentName}", Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                            Text("${record.date} · ${record.deviceId}", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    ExpandSection("Diagnostics & help", "Connection details and troubleshooting", expanded == "Diagnostics & help", { expanded = if (expanded == "Diagnostics & help") "" else "Diagnostics & help" }) {
                        Text("Device: ${if (state.lastConnectionTime == null) "Not verified" else state.deviceId}")
                        Text("Last request: ${diagnostics.lastRequestType ?: "None"}")
                        Text("Response time: ${diagnostics.responseTimeMs?.let { "$it ms" } ?: "—"}")
                        Text("Last success: ${diagnostics.lastSuccessTime ?: "Never"}")
                        diagnostics.lastErrorDetail?.let { Text("Last recorded error: $it", color = MaterialTheme.colorScheme.error) }
                        OutlinedButton(onClick = onRefresh, enabled = !busy) { Text("Test connection") }
                        Text("1. Join Attendance_Bot Wi-Fi.\n2. Confirm the device address under Connection.\n3. Keep this app open for automatic updates.\n4. If time is incorrect, set the clock under Teacher controls.\n\nDownloading a session closes attendance on the existing firmware. History export only exports records already saved on this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (action != null) {
        AlertDialog(
            onDismissRequest = { action = null; pin = "" },
            title = { Text(when (action) { "download" -> "Download attendance"; "reset" -> "Start next lecture?"; else -> "Set ESP32 clock" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(when (action) {
                        "download" -> "This downloads the current session as CSV and closes attendance on the ESP32."
                        "reset" -> "This clears the current attendance on the ESP32 and opens a new session. Saved phone history stays available."
                        else -> "Check the date and time below before sending it to the device."
                    })
                    OutlinedTextField(pin, { pin = it }, label = { Text("Teacher PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    if (action == "clock") Field(dateTime, { dateTime = it }, "YYYY-MM-DD HH:MM:SS")
                    dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = pin.isNotBlank() && !busy, onClick = {
                    when (action) {
                        "download" -> { pendingDownloadPin = pin; download.launch("AttenBot_${LocalDate.now()}.csv") }
                        "reset" -> onReset(pin)
                        "clock" -> {
                            val valid = runCatching { LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)) }.isSuccess
                            if (!valid) { dialogError = "Enter a valid date and time: YYYY-MM-DD HH:MM:SS"; return@TextButton }
                            onClock(pin, dateTime)
                        }
                    }
                    pin = ""; action = null
                }) { Text(if (action == "reset") "Clear & start" else "Continue") }
            },
            dismissButton = { TextButton(onClick = { action = null; pin = "" }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FocusCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun StatPill(text: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Pill) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExpandSection(
    title: String,
    subtitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            TextButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Icon(
                    imageVector = if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (open) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
