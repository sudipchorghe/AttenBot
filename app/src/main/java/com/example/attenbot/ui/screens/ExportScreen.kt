@file:Suppress("unused")

package com.example.attenbot.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.attenbot.ui.components.SettingsDivider
import com.example.attenbot.ui.components.SettingsGroup
import com.example.attenbot.ui.components.SettingsRow
import com.example.attenbot.ui.components.SettingsSection
import com.example.attenbot.ui.state.ExportUiState
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.ButtonShape
import com.example.attenbot.ui.theme.StatusGreen
import com.example.attenbot.ui.theme.StatusRed
import java.io.File

@Composable
fun ExportScreen(
    uiState: ExportUiState,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Export Master CSV",
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
                title = "EXPORT STATUS SUMMARY",
                footer = "Generates a complete CSV file containing all attendance records from the Room database."
            ) {
                SettingsGroup {
                    SettingsRow(
                        title = "Total Database Records",
                        subtitle = "${uiState.totalRecordsToExport} student attendance records available",
                        icon = Icons.Rounded.Folder,
                        iconBackgroundColor = AppleTheme.colors.accent
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "CSV File Headers Format",
                        subtitle = "ID, Date, Roll Number, Student Name",
                        icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                        iconBackgroundColor = StatusGreen
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Target Directory",
                        subtitle = "Downloads/AttenBot (MediaStore / Public Storage)",
                        icon = Icons.Rounded.FileDownload,
                        iconBackgroundColor = AppleTheme.colors.accent
                    )
                }
            }
        }

        item {
            SettingsSection(title = "EXPORT ACTION") {
                SettingsGroup {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Button(
                            onClick = onExportClick,
                            enabled = (!uiState.isExporting) && (uiState.totalRecordsToExport > 0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusGreen,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Generating Master CSV...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Export Master CSV", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }

                        if (uiState.totalRecordsToExport == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Database is empty. Sync attendance data before exporting.",
                                fontSize = 13.sp,
                                color = AppleTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        if (uiState.exportSuccessMessage != null || uiState.exportErrorMessage != null) {
            item {
                SettingsSection(title = "EXPORT CONFIRMATION") {
                    SettingsGroup {
                        if (uiState.exportSuccessMessage != null) {
                            SettingsRow(
                                title = "Export Successful",
                                subtitle = uiState.exportSuccessMessage,
                                icon = Icons.Rounded.CheckCircle,
                                iconBackgroundColor = StatusGreen
                            )

                            if (uiState.lastExportPath != null) {
                                SettingsDivider()
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "FILE LOCATION / URI:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppleTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = ButtonShape,
                                        color = AppleTheme.colors.groupedBackground,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.lastExportPath,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppleTheme.colors.accent,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    if (uiState.lastExportPath.startsWith("/")) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val file = File(uiState.lastExportPath)
                                                    val fileUri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/csv"
                                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Master Attendance CSV"))
                                                } catch (_: Exception) {
                                                    // Provider error fallback
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ButtonShape
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.size(8.dp))
                                            Text("Share CSV File")
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.exportErrorMessage != null) {
                            SettingsRow(
                                title = "Export Failed",
                                subtitle = uiState.exportErrorMessage,
                                icon = Icons.Rounded.Error,
                                iconBackgroundColor = StatusRed
                            )
                        }
                    }
                }
            }
        }
    }
}
