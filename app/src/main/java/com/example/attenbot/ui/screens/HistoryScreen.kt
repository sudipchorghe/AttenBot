@file:Suppress("unused")

package com.example.attenbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.components.SettingsDivider
import com.example.attenbot.ui.components.SettingsGroup
import com.example.attenbot.ui.components.SettingsRow
import com.example.attenbot.ui.components.SettingsSection
import com.example.attenbot.ui.state.HistoryUiState
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.ButtonShape
import com.example.attenbot.ui.theme.PillShape

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSearchQueryChange: (String) -> Unit,
    onYearFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val yearOptions = listOf("All", "2026", "2025", "2024")
    val groupedRecords = uiState.records.groupBy { it.date }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Yearly Attendance History",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTheme.colors.textPrimary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search by Roll Number, Name, or Date (YYYY-MM-DD)...",
                        color = AppleTheme.colors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = ButtonShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppleTheme.colors.accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = AppleTheme.colors.groupedBackground,
                    unfocusedContainerColor = AppleTheme.colors.groupedBackground,
                    focusedTextColor = AppleTheme.colors.textPrimary,
                    unfocusedTextColor = AppleTheme.colors.textPrimary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = AppleTheme.colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                tint = AppleTheme.colors.textSecondary
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(yearOptions) { year ->
                    val isSelected = uiState.selectedYearFilter == year
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                color = if (isSelected) AppleTheme.colors.accent else AppleTheme.colors.groupedBackground
                            )
                            .clickable { onYearFilterChange(year) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = year,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else AppleTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DATABASE RECORDS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleTheme.colors.textSecondary
                )
                Surface(
                    shape = PillShape,
                    color = AppleTheme.colors.accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${uiState.records.size} Records Found",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTheme.colors.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (groupedRecords.isEmpty()) {
            item {
                SettingsSection(title = "DATABASE STATUS") {
                    SettingsGroup {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (uiState.searchQuery.isNotBlank()) Icons.Rounded.SearchOff else Icons.Rounded.FolderOff,
                                contentDescription = null,
                                tint = AppleTheme.colors.textSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Attendance Records Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank())
                                    "No records match query '${uiState.searchQuery}'. Try searching by Roll Number, Name, or Date format (YYYY-MM-DD)."
                                else
                                    "The database is currently empty. Sync attendance data from the Live Sync Dashboard to populate records.",
                                fontSize = 13.sp,
                                color = AppleTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            groupedRecords.forEach { (date, dateRecords) ->
                item(key = date) {
                    SettingsSection(
                        title = "DATE: $date (${dateRecords.size} ${if (dateRecords.size == 1) "STUDENT" else "STUDENTS"})"
                    ) {
                        SettingsGroup {
                            dateRecords.forEachIndexed { index, record ->
                                SettingsRow(
                                    title = record.studentName,
                                    subtitle = "Roll Number: ${record.rollNumber}",
                                    icon = Icons.Rounded.Person,
                                    iconBackgroundColor = AppleTheme.colors.accent,
                                    trailingContent = {
                                        Surface(
                                            shape = PillShape,
                                            color = AppleTheme.colors.groupedBackground
                                        ) {
                                            Text(
                                                text = "Roll: ${record.rollNumber}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppleTheme.colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                )
                                if (index < (dateRecords.size - 1)) {
                                    SettingsDivider(startIndent = 58.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
