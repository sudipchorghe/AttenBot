package com.example.attenbot.ui.state

import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.remote.model.ApiAttendanceRecord
import com.example.attenbot.data.repository.SyncResult

data class SyncUiState(
    val baseUrl: String = "http://192.168.4.1/",
    val esp32Url: String = "http://192.168.4.1/",
    val deviceId: String = "CLASSROOM_A",
    val closed: Boolean = false,
    val count: Int = 0,
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val isResetting: Boolean = false,
    val isSettingRtcTime: Boolean = false,
    val isConnected: Boolean = true,
    val lastSyncTime: String? = null,
    val lastConnectionTime: String? = null,
    val statusText: String = "Ready",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val warningMessage: String? = null,
    val isShowingCachedData: Boolean = false,
    val liveRecords: List<ApiAttendanceRecord> = emptyList(),
    val activeSessionRecords: List<AttendanceRecord> = emptyList(),
    val totalRecordsCount: Int = 0,
    val syncResult: SyncResult? = null
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

data class DiagnosticsState(
    val lastStatusCode: Int? = null,
    val lastRequestType: String? = null,
    val lastSuccessTime: String? = null,
    val lastErrorDetail: String? = null,
    val responseTimeMs: Long? = null,
    val wifiConnected: Boolean = true,
    val baseUrlValid: Boolean = true,
    val internetPermissionOk: Boolean = true,
    val cleartextTrafficOk: Boolean = true
)
