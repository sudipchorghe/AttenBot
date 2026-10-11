package com.example.attenbot.ui.state

import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.repository.SyncResult

data class SyncUiState(
    val isSyncing: Boolean = false,
    val isConnected: Boolean = true,
    val esp32Url: String = "http://192.168.4.1/download",
    val lastSyncTime: String? = null,
    val syncResult: SyncResult? = null,
    val totalRecordsCount: Int = 0,
    val activeSessionRecords: List<AttendanceRecord> = emptyList(),
    val statusText: String = "Ready to Sync"
)

data class HistoryUiState(
    val searchQuery: String = "",
    val records: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val selectedYearFilter: String = "All"
)

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportErrorMessage: String? = null,
    val totalRecordsToExport: Int = 0,
    val lastExportPath: String? = null
)
