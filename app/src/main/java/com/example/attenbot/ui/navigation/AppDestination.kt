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
    object Sync : AppDestination("sync", "Sync Dashboard", Icons.Rounded.Sync)
    object History : AppDestination("history", "Yearly History", Icons.Rounded.DateRange)
    object Export : AppDestination("export", "Export CSV", Icons.Rounded.FileDownload)

    companion object {
        val entries = listOf(Sync, History, Export)
    }
}
