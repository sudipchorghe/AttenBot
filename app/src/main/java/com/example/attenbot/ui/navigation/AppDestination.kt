package com.example.attenbot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Sync : AppDestination(ROUTE_SYNC, "Sync Dashboard", Icons.Rounded.Sync)
    object History : AppDestination(ROUTE_HISTORY, "Yearly History", Icons.Rounded.DateRange)
    object Export : AppDestination(ROUTE_EXPORT, "Export CSV", Icons.Rounded.FileDownload)

    companion object {
        const val ROUTE_SYNC = "sync"
        const val ROUTE_HISTORY = "history"
        const val ROUTE_EXPORT = "export"

        val entries: List<AppDestination> by lazy {
            listOf(Sync, History, Export)
        }
    }
}
