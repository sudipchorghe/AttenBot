package com.example.attenbot.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attenbot.data.repository.AttendanceRepository
import com.example.attenbot.data.repository.SyncResult
import com.example.attenbot.ui.state.ExportUiState
import com.example.attenbot.ui.state.HistoryUiState
import com.example.attenbot.ui.state.SyncUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedYearFilter = MutableStateFlow("All")

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyUiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _selectedYearFilter,
        _searchQuery.flatMapLatest { query -> repository.searchRecords(query) }
    ) { query, yearFilter, records ->
        val filteredRecords = if (yearFilter == "All") {
            records
        } else {
            records.filter { it.date.startsWith(yearFilter) }
        }
        HistoryUiState(
            searchQuery = query,
            records = filteredRecords,
            isLoading = false,
            selectedYearFilter = yearFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    private val _exportUiState = MutableStateFlow(ExportUiState())
    val exportUiState: StateFlow<ExportUiState> = _exportUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                _syncUiState.update { it.copy(totalRecordsCount = records.size) }
                _exportUiState.update { it.copy(totalRecordsToExport = records.size) }
            }
        }
    }

    fun updateEsp32Url(url: String) {
        _syncUiState.update { it.copy(esp32Url = url) }
    }

    fun syncAttendance(overrideDate: String? = null) {
        val currentUrl = _syncUiState.value.esp32Url
        _syncUiState.update {
            it.copy(
                isSyncing = true,
                syncResult = null,
                statusText = "Syncing..."
            )
        }

        viewModelScope.launch {
            val result = repository.syncWithEsp32(url = currentUrl, overrideDate = overrideDate)
            val now = SimpleDateFormat("HH:mm:ss, MMM dd", Locale.getDefault()).format(Date())
            val isSuccess = result is SyncResult.Success

            val newStatusText = if (isSuccess) "Success" else "Connection Error"
            val downloaded = if (result is SyncResult.Success) result.downloadedRecords else emptyList()

            _syncUiState.update {
                it.copy(
                    isSyncing = false,
                    isConnected = isSuccess,
                    syncResult = result,
                    lastSyncTime = if (isSuccess) now else it.lastSyncTime,
                    statusText = newStatusText,
                    activeSessionRecords = if (isSuccess) downloaded else it.activeSessionRecords
                )
            }
        }
    }

    fun syncWithEsp32(overrideDate: String? = null) {
        syncAttendance(overrideDate)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onSearchQueryChange(query: String) {
        updateSearchQuery(query)
    }

    fun onYearFilterChange(year: String) {
        _selectedYearFilter.value = year
    }

    fun exportMasterCsv(context: Context, targetUri: Uri? = null) {
        _exportUiState.update {
            it.copy(
                isExporting = true,
                exportSuccessMessage = null,
                exportErrorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val records = repository.getRecordsForExport()
                if (records.isEmpty()) {
                    val noDataMsg = "No attendance records found to export."
                    _exportUiState.update {
                        it.copy(
                            isExporting = false,
                            exportErrorMessage = noDataMsg
                        )
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, noDataMsg, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val csvContent = buildString {
                    append("ID,Date,Roll Number,Student Name\n")
                    records.forEach { r ->
                        val cleanName = if (r.studentName.contains(",")) "\"${r.studentName}\"" else r.studentName
                        append("${r.id},${r.date},${r.rollNumber},$cleanName\n")
                    }
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "AttenBot_Export_$timestamp.csv"

                var resultPathOrUri: String? = null

                withContext(Dispatchers.IO) {
                    if (targetUri != null) {
                        context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                            outputStream.write(csvContent.toByteArray())
                        }
                        resultPathOrUri = targetUri.toString()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AttenBot")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(csvContent.toByteArray())
                            }
                            resultPathOrUri = uri.toString()
                        } else {
                            throw IOException("Failed to create MediaStore entry for Downloads")
                        }
                    } else {
                        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                        if (!downloadsDir.exists()) downloadsDir.mkdirs()
                        val file = File(downloadsDir, fileName)
                        FileOutputStream(file).use { outputStream ->
                            outputStream.write(csvContent.toByteArray())
                        }
                        resultPathOrUri = file.absolutePath
                    }
                }

                val successMsg = "Successfully exported ${records.size} records to $fileName"

                _exportUiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccessMessage = successMsg,
                        lastExportPath = resultPathOrUri
                    )
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMsg = "Failed to export: ${e.localizedMessage ?: "Unknown error"}"
                _exportUiState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = errorMsg
                    )
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun clearSyncResult() {
        _syncUiState.update {
            it.copy(
                syncResult = null,
                statusText = "Ready to Sync"
            )
        }
    }

    fun clearExportResult() {
        _exportUiState.update { it.copy(exportSuccessMessage = null, exportErrorMessage = null) }
    }
}

class AttendanceViewModelFactory(
    private val repository: AttendanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
