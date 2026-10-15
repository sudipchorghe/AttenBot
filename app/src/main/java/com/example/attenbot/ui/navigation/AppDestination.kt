package com.example.attenbot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Sync : AppDestination(ROUTE_SYNC, "Live Sync", Icons.Rounded.Sync)
    object Teacher : AppDestination(ROUTE_TEACHER, "Teacher Controls", Icons.Rounded.Security)
    object RtcTime : AppDestination(ROUTE_RTC, "Set Clock", Icons.Rounded.Schedule)
    object Troubleshooting : AppDestination(ROUTE_TROUBLESHOOTING, "Diagnostics", Icons.Rounded.Build)
    object Help : AppDestination(ROUTE_HELP, "Help & Settings", Icons.AutoMirrored.Rounded.HelpOutline)
    object History : AppDestination(ROUTE_HISTORY, "Attendance History", Icons.Rounded.DateRange)

    companion object {
        const val ROUTE_SYNC = "sync"
        const val ROUTE_TEACHER = "teacher"
        const val ROUTE_RTC = "rtc_time"
        const val ROUTE_TROUBLESHOOTING = "troubleshooting"
        const val ROUTE_HELP = "help"
        const val ROUTE_HISTORY = "history"

        val drawerEntries: List<AppDestination> by lazy {
            listOf(Teacher, RtcTime, Troubleshooting, History, Help)
        }
    }
}
