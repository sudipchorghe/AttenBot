package com.example.attenbot.ui.screens

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    onSyncClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val computedStatusText = when {
        uiState.isSyncing -> "Syncing..."
        uiState.syncResult is SyncResult.Success -> "Success"
        uiState.syncResult is SyncResult.Error -> "Connection Error"
        else -> uiState.statusText
    }

    val displayEndpoint = if (uiState.esp32Url.contains("192.168.4.1")) {
        "${uiState.esp32Url} - Attendance_Bot AP"
    } else {
        uiState.esp32Url
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
                text = "Live Sync Dashboard",
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
                deviceIpOrUrl = displayEndpoint,
                isConnected = uiState.isConnected && uiState.syncResult !is SyncResult.Error,
                lastSyncTime = uiState.lastSyncTime ?: "Not synced yet",
                totalRecords = uiState.totalRecordsCount,
                isSyncing = uiState.isSyncing
            )
        }

        val currentResult = uiState.syncResult
        if (currentResult is SyncResult.Error) {
            item {
                SettingsSection(title = "Connection Error Warning") {
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
                                    text = "Sync Failed",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRed,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentResult.message,
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
                                    text = "⚠️ Guidance: Ensure you are connected to the ESP32 Attendance_Bot Wi-Fi network (http://192.168.4.1). Check that your device Wi-Fi is enabled and connected to the Attendance_Bot AP.",
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
                title = "ESP32 Wi-Fi Endpoint",
                footer = "Default target endpoint is http://192.168.4.1/download (Attendance_Bot AP)"
            ) {
                SettingsGroup {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "TARGET SERVER URL",
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
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
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

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onSyncClick()
                            },
                            enabled = !uiState.isSyncing && uiState.esp32Url.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleTheme.colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Syncing with ESP32...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Sync Attendance", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        if (currentResult is SyncResult.Success) {
            item {
                SettingsSection(title = "Sync Result Summary") {
                    SettingsGroup {
                        SettingsRow(
                            title = "Sync Completed Successfully",
                            subtitle = "Fetched ${currentResult.totalFetched} records from ESP32",
                            icon = Icons.Rounded.CheckCircle,
                            iconBackgroundColor = StatusGreen
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "New Records Inserted",
                            trailingContent = {
                                Text(
                                    text = "+${currentResult.insertedCount}",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen,
                                    fontSize = 16.sp
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Duplicates Skipped",
                            trailingContent = {
                                Text(
                                    text = "${currentResult.skippedCount}",
                                    fontWeight = FontWeight.Medium,
                                    color = AppleTheme.colors.textSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "ACTIVE SESSION DOWNLOADS",
                footer = if (uiState.activeSessionRecords.isNotEmpty()) "Showing ${uiState.activeSessionRecords.size} records downloaded in the active session." else null
            ) {
                SettingsGroup {
                    if (uiState.activeSessionRecords.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = AppleTheme.colors.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No records downloaded in active session",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Sync Attendance' above to fetch records from ESP32.",
                                fontSize = 13.sp,
                                color = AppleTheme.colors.textSecondary
                            )
                        }
                    } else {
                        uiState.activeSessionRecords.forEachIndexed { index, record ->
                            SettingsRow(
                                title = record.studentName,
                                subtitle = "Roll No: ${record.rollNumber} • Date: ${record.date}",
                                icon = Icons.Rounded.Person,
                                iconBackgroundColor = AppleTheme.colors.accent,
                                trailingContent = {
                                    Surface(
                                        shape = PillShape,
                                        color = AppleTheme.colors.accent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = AppleTheme.colors.accent,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            )
                            if (index < uiState.activeSessionRecords.size - 1) {
                                SettingsDivider(startIndent = 58.dp)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Navigation & Quick Actions") {
                SettingsGroup {
                    SettingsRow(
                        title = "Yearly Attendance History",
                        subtitle = "Browse, search, and filter student records",
                        icon = Icons.Rounded.DateRange,
                        iconBackgroundColor = AppleTheme.colors.accent,
                        showChevron = true,
                        onClick = onNavigateToHistory
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Export Master CSV",
                        subtitle = "Generate and save complete CSV export",
                        icon = Icons.Rounded.FileDownload,
                        iconBackgroundColor = StatusGreen,
                        showChevron = true,
                        onClick = onNavigateToExport
                    )
                }
            }
        }
    }
}
