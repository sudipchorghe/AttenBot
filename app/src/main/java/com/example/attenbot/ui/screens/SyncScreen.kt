@file:Suppress("unused")

package com.example.attenbot.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.data.repository.SyncResult
import com.example.attenbot.ui.components.SettingsDivider
import com.example.attenbot.ui.components.SettingsGroup
import com.example.attenbot.ui.components.SettingsRow
import com.example.attenbot.ui.components.SettingsSection
import com.example.attenbot.ui.components.StatusHeaderTile
import com.example.attenbot.ui.state.SyncUiState
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.ButtonShape
import com.example.attenbot.ui.theme.PillShape
import com.example.attenbot.ui.theme.StatusGreen
import com.example.attenbot.ui.theme.StatusRed

@Composable
fun SyncScreen(
    uiState: SyncUiState,
    onUrlChange: (String) -> Unit,
    onSaveUrlClick: (String) -> Unit,
    onConnectClick: () -> Unit,
    onTestConnectionClick: () -> Unit,
    onRefreshStatusClick: () -> Unit,
    onRefreshAttendanceClick: () -> Unit,
    onDownloadCsvClick: (pin: String, uri: Uri) -> Unit,
    onResetAttendanceClick: (pin: String) -> Unit,
    onNavigateToTeacherControls: () -> Unit,
    onNavigateToRtc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var showDownloadConfirmDialog by remember { mutableStateOf(false) }
    var showDownloadPinDialog by remember { mutableStateOf(false) }
    var downloadPinInput by remember { mutableStateOf("") }
    var pendingDownloadPin by remember { mutableStateOf<String?>(null) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var resetPinInput by remember { mutableStateOf("") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val pin = pendingDownloadPin
        pendingDownloadPin = null
        if ((uri != null) && !pin.isNullOrBlank()) {
            onDownloadCsvClick(pin, uri)
        }
    }

    val computedStatusText = when {
        uiState.isConnecting -> "Connecting..."
        uiState.isRefreshing -> "Refreshing..."
        uiState.isSyncing -> "Syncing..."
        uiState.syncResult is SyncResult.Success -> "Success"
        uiState.syncResult is SyncResult.Error || !uiState.isConnected -> "Connection Error"
        else -> uiState.statusText
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Attendance Dashboard",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTheme.colors.textPrimary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            StatusHeaderTile(
                statusText = computedStatusText,
                deviceIpOrUrl = uiState.baseUrl,
                isConnected = uiState.isConnected && uiState.syncResult !is SyncResult.Error,
                lastSyncTime = uiState.lastConnectionTime ?: uiState.lastSyncTime ?: "Not connected yet",
                totalRecords = uiState.count,
                isSyncing = uiState.isConnecting || uiState.isRefreshing || uiState.isSyncing
            )
        }

        if (uiState.errorMessage != null || uiState.syncResult is SyncResult.Error) {
            val errorText = uiState.errorMessage ?: (uiState.syncResult as? SyncResult.Error)?.message ?: "Connection error"
            item {
                SettingsSection(title = "Connection Diagnostic Warning") {
                    SettingsGroup {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = StatusRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connection Failure",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRed,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorText,
                                fontSize = 14.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = ButtonShape,
                                color = StatusRed.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Guidance: Ensure your phone is connected to the ESP32 Wi-Fi network (http://192.168.4.1/). Verify IP address and cleartext HTTP settings.",
                                    fontSize = 13.sp,
                                    color = StatusRed,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "ESP32 DEVICE CONNECTION",
                footer = "Default URL is http://192.168.4.1/. URLs are normalized to end with '/'. Do not store teacher PIN."
            ) {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ESP32 BASE URL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppleTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = uiState.esp32Url,
                            onValueChange = onUrlChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = ButtonShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppleTheme.colors.accent,
                                unfocusedBorderColor = AppleTheme.colors.divider,
                                focusedTextColor = AppleTheme.colors.textPrimary,
                                unfocusedTextColor = AppleTheme.colors.textPrimary
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = AppleTheme.colors.textSecondary
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    onSaveUrlClick(uiState.esp32Url)
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = ButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AppleTheme.colors.accent)
                            ) {
                                Text("Save URL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    onConnectClick()
                                },
                                enabled = !uiState.isConnecting,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = ButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                            ) {
                                Text("Connect", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onTestConnectionClick,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = ButtonShape
                            ) {
                                Icon(imageVector = Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Connection", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onRefreshStatusClick,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = ButtonShape
                            ) {
                                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh Status", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "ATTENDANCE CONTROLS") {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onRefreshAttendanceClick,
                                enabled = !uiState.isRefreshing,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = ButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AppleTheme.colors.accent)
                            ) {
                                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showDownloadConfirmDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download CSV", fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToRtc,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = ButtonShape
                            ) {
                                Icon(imageVector = Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set RTC Time", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onNavigateToTeacherControls,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = ButtonShape
                            ) {
                                Icon(imageVector = Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Teacher Controls", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SettingsDivider(startIndent = 0.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Attendance for Next Lecture", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "LIVE ATTENDANCE RECORDS",
                footer = if (uiState.isShowingCachedData) "Offline cached data — not confirmed live." else null
            ) {
                SettingsGroup {
                    val records = uiState.liveRecords
                    if (records.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = AppleTheme.colors.textSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No attendance records found.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Refresh Records' to fetch active student submissions.",
                                fontSize = 13.sp,
                                color = AppleTheme.colors.textSecondary
                            )
                        }
                    } else {
                        records.forEachIndexed { index, rec ->
                            SettingsRow(
                                title = rec.name.ifBlank { "Unknown Student" },
                                subtitle = "Roll No: ${rec.rollNumber} • Time: ${rec.timestamp} • Device: ${rec.deviceId.ifBlank { uiState.deviceId }}",
                                icon = Icons.Rounded.Person,
                                iconBackgroundColor = AppleTheme.colors.accent,
                                trailingContent = {
                                    Surface(
                                        shape = PillShape,
                                        color = AppleTheme.colors.accent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Roll #${rec.rollNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = AppleTheme.colors.accent,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            )
                            if (index < records.size - 1) {
                                SettingsDivider(startIndent = 58.dp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = ButtonShape,
                color = AppleTheme.colors.groupedBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = AppleTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Student Cookie Limitation Notice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AppleTheme.colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browser cookies help prevent accidental duplicate submissions, but they are not a secure physical-device identity. Students may bypass this by clearing cookies, using another browser, using private browsing, or using another device.",
                        fontSize = 12.sp,
                        color = AppleTheme.colors.textSecondary
                    )
                }
            }
        }
    }

    if (showDownloadConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = AppleTheme.colors.statusOrange
                )
            },
            title = { Text("Download Attendance CSV", fontWeight = FontWeight.Bold) },
            text = { Text("Downloading the CSV will close attendance on this ESP32. Continue?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadConfirmDialog = false
                        showDownloadPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTheme.colors.accent)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDownloadConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDownloadPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showDownloadPinDialog = false
                downloadPinInput = ""
            },
            icon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null, tint = AppleTheme.colors.accent) },
            title = { Text("Enter Teacher PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter administrator teacher PIN to download CSV:", fontSize = 14.sp, color = AppleTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = downloadPinInput,
                        onValueChange = { downloadPinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teacher PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pin = downloadPinInput
                        showDownloadPinDialog = false
                        downloadPinInput = ""
                        pendingDownloadPin = pin
                        val filename = "attendance_${uiState.deviceId.ifBlank { "CLASSROOM_A" }}.csv"
                        createDocumentLauncher.launch(filename)
                    },
                    enabled = downloadPinInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTheme.colors.accent)
                ) {
                    Text("Authorize & Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDownloadPinDialog = false
                        downloadPinInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = { Icon(imageVector = Icons.Rounded.DeleteForever, contentDescription = null, tint = StatusRed) },
            title = { Text("Clear Attendance for Next Lecture?", fontWeight = FontWeight.Bold, color = StatusRed) },
            text = {
                Text(
                    "This will permanently delete the current attendance records from the ESP32 and reopen attendance for the next lecture. Continue?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        showResetPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetPinDialog = false
                resetPinInput = ""
            },
            icon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null, tint = StatusRed) },
            title = { Text("Enter Teacher PIN to Clear", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter administrator teacher PIN to clear current lecture attendance:", fontSize = 14.sp, color = AppleTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetPinInput,
                        onValueChange = { resetPinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teacher PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pin = resetPinInput
                        showResetPinDialog = false
                        resetPinInput = ""
                        onResetAttendanceClick(pin)
                    },
                    enabled = resetPinInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Clear Attendance")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetPinDialog = false
                        resetPinInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
