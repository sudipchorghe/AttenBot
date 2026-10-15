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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.components.SettingsGroup
import com.example.attenbot.ui.components.SettingsSection
import com.example.attenbot.ui.state.SyncUiState
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.ButtonShape
import com.example.attenbot.ui.theme.StatusRed

@Composable
fun HelpScreen(
    uiState: SyncUiState,
    onUrlChange: (String) -> Unit,
    onSaveUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var urlInput by remember(uiState.baseUrl) { mutableStateOf(uiState.baseUrl) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Help & Settings",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTheme.colors.textPrimary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            SettingsSection(
                title = "APP SETTINGS & BASE URL",
                footer = "Configure the default ESP32 base URL used for connecting across classrooms."
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
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                onUrlChange(it)
                            },
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

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onSaveUrlClick(urlInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleTheme.colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save Base URL", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = ButtonShape,
                color = StatusRed.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CRITICAL BACKUP WARNING",
                            fontWeight = FontWeight.Bold,
                            color = StatusRed,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Always download and back up the CSV before clearing attendance. Clearing attendance permanently deletes the current records from the ESP32.",
                            fontSize = 13.sp,
                            color = AppleTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "OPERATIONAL GUIDE") {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        HelpSectionItem(
                            title = "1. Before Class Setup",
                            content = "Power on the ESP32-S3 device and ensure its Wi-Fi Access Point is broadcasting. Connect your phone to the ESP32 Wi-Fi network (e.g., Attendance_Bot)."
                        )
                        HelpSectionItem(
                            title = "2. Connecting to ESP32",
                            content = "In AttenBot, verify or set the base URL in Help & Settings to http://192.168.4.1/. On the Live Sync screen, tap 'Connect' or 'Refresh Status'."
                        )
                        HelpSectionItem(
                            title = "3. Student Attendance Flow",
                            content = "Students connect to the ESP32 Wi-Fi in their phone browser at http://192.168.4.1/, enter their Name and Roll Number, and submit."
                        )
                        HelpSectionItem(
                            title = "4. View Live Attendance",
                            content = "On the Live Sync screen in AttenBot, tap 'Refresh Records' to inspect active submissions."
                        )
                        HelpSectionItem(
                            title = "5. Download Attendance CSV",
                            content = "Under Teacher Controls, tap 'Download CSV', enter your Teacher PIN, and pick a save location. Downloading CSV closes attendance on the ESP32."
                        )
                        HelpSectionItem(
                            title = "6. Clear Attendance for Next Lecture",
                            content = "After downloading the CSV, tap 'Clear Attendance for Next Lecture' and enter your Teacher PIN to clear records on the ESP32 and reopen attendance."
                        )
                        HelpSectionItem(
                            title = "7. Set DS3231 RTC Time",
                            content = "Go to 'Set Clock', verify or adjust the phone date and time, enter Teacher PIN, and tap 'Set RTC Time'."
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "SECURITY LIMITATIONS & NOTICE") {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = AppleTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Local Network & Cookie Scope",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Browser cookies help prevent accidental duplicate submissions, but they are not a secure physical-device identity. Students may bypass this by clearing cookies, using another browser, using private browsing, or using another device.",
                            fontSize = 13.sp,
                            color = AppleTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This system uses local HTTP and basic PIN protection. It is intended for a trusted local classroom network. It does not provide encrypted internet-grade authentication.",
                            fontSize = 13.sp,
                            color = AppleTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSectionItem(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = AppleTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            color = AppleTheme.colors.textSecondary
        )
    }
}
