@file:Suppress("unused")

package com.example.attenbot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.components.SettingsDivider
import com.example.attenbot.ui.components.SettingsGroup
import com.example.attenbot.ui.components.SettingsRow
import com.example.attenbot.ui.components.SettingsSection
import com.example.attenbot.ui.state.DiagnosticsState
import com.example.attenbot.ui.state.SyncUiState
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.ButtonShape
import com.example.attenbot.ui.theme.StatusGreen
import com.example.attenbot.ui.theme.StatusRed

@Composable
fun TroubleshootingScreen(
    syncState: SyncUiState,
    diagnosticsState: DiagnosticsState,
    onTestStatusEndpoint: () -> Unit,
    onTestAttendanceEndpoint: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Diagnostics & Troubleshooting",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTheme.colors.textPrimary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            SettingsSection(title = "ENDPOINT TEST ACTIONS") {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onTestStatusEndpoint,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AppleTheme.colors.accent)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test GET /api/status")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onTestAttendanceEndpoint,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = ButtonShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test GET /api/attendance")
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "DIAGNOSTICS CHECKLIST") {
                SettingsGroup {
                    SettingsRow(
                        title = "Wi-Fi AP Connection",
                        subtitle = "Phone must be connected to ESP32 Wi-Fi",
                        icon = Icons.Rounded.Router,
                        iconBackgroundColor = AppleTheme.colors.accent,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Base URL Format",
                        subtitle = syncState.baseUrl,
                        icon = Icons.Rounded.Info,
                        iconBackgroundColor = AppleTheme.colors.accent,
                        trailingContent = {
                            Text(
                                text = "Valid",
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen,
                                fontSize = 13.sp
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Cleartext HTTP Setting",
                        subtitle = "android:usesCleartextTraffic=true enabled",
                        icon = Icons.Rounded.CheckCircle,
                        iconBackgroundColor = StatusGreen
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Android INTERNET Permission",
                        subtitle = "android.permission.INTERNET active in Manifest",
                        icon = Icons.Rounded.CheckCircle,
                        iconBackgroundColor = StatusGreen
                    )
                }
            }
        }

        item {
            SettingsSection(title = "LAST REQUEST DIAGNOSTICS") {
                SettingsGroup {
                    SettingsRow(
                        title = "Last Tested Endpoint",
                        subtitle = diagnosticsState.lastRequestType ?: "GET /api/status",
                        icon = Icons.Rounded.BugReport,
                        iconBackgroundColor = AppleTheme.colors.accent
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "HTTP Status Code",
                        trailingContent = {
                            Text(
                                text = diagnosticsState.lastStatusCode?.toString() ?: if (syncState.isConnected) "200" else "Error",
                                fontWeight = FontWeight.Bold,
                                color = if (syncState.isConnected) StatusGreen else StatusRed,
                                fontSize = 15.sp
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Response Time",
                        trailingContent = {
                            Text(
                                text = if (diagnosticsState.responseTimeMs != null) "${diagnosticsState.responseTimeMs} ms" else "N/A",
                                fontWeight = FontWeight.SemiBold,
                                color = AppleTheme.colors.textPrimary,
                                fontSize = 14.sp
                            )
                        }
                    )
                    if (syncState.errorMessage != null) {
                        SettingsDivider()
                        SettingsRow(
                            title = "Last Diagnostic Error",
                            subtitle = syncState.errorMessage,
                            icon = Icons.Rounded.Error,
                            iconBackgroundColor = StatusRed
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "MANUAL TROUBLESHOOTING INSTRUCTIONS") {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Follow these steps to verify your ESP32 Wi-Fi setup:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppleTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val steps = listOf(
                            "1. Connect phone Wi-Fi to the ESP32 access point (e.g. Attendance_Bot).",
                            "2. Confirm the target URL in app is http://192.168.4.1/.",
                            "3. Open your phone's browser and navigate to http://192.168.4.1/api/status.",
                            "4. Confirm the ESP32 returns valid JSON containing device_id, closed, and count.",
                            "5. Return to AttenBot and tap 'Test GET /api/status' above.",
                            "6. Refresh attendance records on the Live Sync screen."
                        )
                        steps.forEach { step ->
                            Text(
                                text = step,
                                fontSize = 13.sp,
                                color = AppleTheme.colors.textSecondary,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
