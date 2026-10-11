package com.example.attenbot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.GroupedContainerShape

@Composable
fun StatusHeaderTile(
    statusText: String,
    deviceIpOrUrl: String,
    modifier: Modifier = Modifier,
    isConnected: Boolean = true,
    lastSyncTime: String? = null,
    totalRecords: Int? = null,
    isSyncing: Boolean = false
) {
    val targetColor = when {
        isSyncing || statusText == "Syncing..." -> AppleTheme.colors.statusOrange
        statusText == "Success" -> AppleTheme.colors.statusGreen
        statusText == "Connection Error" || !isConnected -> AppleTheme.colors.statusRed
        else -> AppleTheme.colors.accent
    }

    val statusColor by animateColorAsState(
        targetValue = targetColor,
        label = "statusColor"
    )

    val iconVector = when {
        isSyncing || statusText == "Syncing..." -> Icons.Rounded.Sync
        statusText == "Success" -> Icons.Rounded.CheckCircle
        statusText == "Connection Error" || !isConnected -> Icons.Rounded.ErrorOutline
        else -> Icons.Rounded.Wifi
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(GroupedContainerShape),
        color = AppleTheme.colors.groupedBackground,
        shape = GroupedContainerShape,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = statusText,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                        }

                        Text(
                            text = deviceIpOrUrl,
                            fontSize = 13.sp,
                            color = AppleTheme.colors.textSecondary
                        )
                    }
                }
            }

            if ((lastSyncTime != null) || (totalRecords != null)) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsDivider(startIndent = 0.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (lastSyncTime != null) {
                        Column {
                            Text(
                                text = "LAST SYNC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppleTheme.colors.textSecondary
                            )
                            Text(
                                text = lastSyncTime,
                                fontSize = 14.sp,
                                color = AppleTheme.colors.textPrimary
                            )
                        }
                    }

                    if (totalRecords != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL RECORDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppleTheme.colors.textSecondary
                            )
                            Text(
                                text = totalRecords.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTheme.colors.accent
                            )
                        }
                    }
                }
            }
        }
    }
}
