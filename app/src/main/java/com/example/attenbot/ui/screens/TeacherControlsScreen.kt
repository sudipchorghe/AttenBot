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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.components.SettingsDivider
import com.example.attenbot.ui.components.SettingsGroup
import com.example.attenbot.ui.components.SettingsRow
import com.example.attenbot.ui.components.SettingsSection
import com.example.attenbot.ui.state.SyncUiState
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.ButtonShape
import com.example.attenbot.ui.theme.StatusGreen
import com.example.attenbot.ui.theme.StatusRed

@Composable
fun TeacherControlsScreen(
    uiState: SyncUiState,
    onDownloadCsvClick: (pin: String, uri: Uri) -> Unit,
    onResetAttendanceClick: (pin: String) -> Unit,
    onNavigateToRtc: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        if (uri != null && !pin.isNullOrBlank()) {
            onDownloadCsvClick(pin, uri)
        }
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
                text = "Teacher Controls",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTheme.colors.textPrimary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            SettingsSection(title = "DEVICE STATUS") {
                SettingsGroup {
                    SettingsRow(
                        title = "Device ID / Classroom",
                        trailingContent = {
                            Text(
                                text = uiState.deviceId,
                                fontWeight = FontWeight.Bold,
                                color = AppleTheme.colors.accent,
                                fontSize = 15.sp
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Attendance Status",
                        trailingContent = {
                            Text(
                                text = if (uiState.closed) "CLOSED" else "OPEN",
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.closed) StatusRed else StatusGreen,
                                fontSize = 15.sp
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Current Attendance Count",
                        trailingContent = {
                            Text(
                                text = "${uiState.count} Records",
                                fontWeight = FontWeight.Bold,
                                color = AppleTheme.colors.textPrimary,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = "1. DOWNLOAD ATTENDANCE CSV",
                footer = "Requires Teacher PIN. Downloading CSV closes attendance on the ESP32. Students cannot submit attendance after download."
            ) {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                onClearMessages()
                                showDownloadConfirmDialog = true
                            },
                            enabled = !uiState.isDownloading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleTheme.colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading CSV...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download CSV", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "2. SET DS3231 RTC TIME",
                footer = "Sync the ESP32 internal clock with your phone's date and time."
            ) {
                SettingsGroup {
                    SettingsRow(
                        title = "Set RTC Clock Time",
                        subtitle = "Configure DS3231 hardware timestamp module",
                        icon = Icons.Rounded.Schedule,
                        iconBackgroundColor = AppleTheme.colors.accent,
                        showChevron = true,
                        onClick = onNavigateToRtc
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = "3. DESTRUCTIVE ACTION - CLEAR ATTENDANCE",
                footer = "Warning: Permanently deletes all current attendance records from the ESP32 and reopens attendance for the next lecture. Make sure to download CSV first."
            ) {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                onClearMessages()
                                showResetConfirmDialog = true
                            },
                            enabled = !uiState.isResetting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRed,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isResetting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clearing Attendance...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Clear Attendance for Next Lecture",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.successMessage != null) {
            item {
                Surface(
                    shape = ButtonShape,
                    color = StatusGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.successMessage,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Surface(
                    shape = ButtonShape,
                    color = StatusRed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage,
                            color = StatusRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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
                            text = "Security Notice & Limitations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
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
            title = {
                Text(text = "Download Attendance CSV", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Downloading the CSV will close attendance on this ESP32. Continue?",
                    fontSize = 14.sp
                )
            },
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
                OutlinedButton(onClick = { showDownloadConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDownloadPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showDownloadPinDialog = false
                downloadPinInput = ""
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = AppleTheme.colors.accent
                )
            },
            title = {
                Text(text = "Enter Teacher PIN", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter the administrator teacher PIN to authorize CSV download:",
                        fontSize = 14.sp,
                        color = AppleTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = downloadPinInput,
                        onValueChange = { downloadPinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teacher PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleTheme.colors.accent
                        )
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
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = StatusRed
                )
            },
            title = {
                Text(
                    text = "Clear Attendance for Next Lecture?",
                    fontWeight = FontWeight.Bold,
                    color = StatusRed
                )
            },
            text = {
                Text(
                    text = "This will permanently delete the current attendance records from the ESP32 and reopen attendance for the next lecture. Continue?",
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
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetPinDialog = false
                resetPinInput = ""
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = StatusRed
                )
            },
            title = {
                Text(text = "Enter Teacher PIN to Clear", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter administrator teacher PIN to clear current lecture attendance:",
                        fontSize = 14.sp,
                        color = AppleTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetPinInput,
                        onValueChange = { resetPinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teacher PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusRed
                        )
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
