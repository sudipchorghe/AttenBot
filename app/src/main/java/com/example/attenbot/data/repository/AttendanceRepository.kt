package com.example.attenbot.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.parser.CsvParser
import com.example.attenbot.data.remote.Esp32NetworkClient
import com.example.attenbot.data.remote.NetworkResult
import com.example.attenbot.data.remote.model.ApiAttendanceRecord
import com.example.attenbot.data.remote.model.Esp32Status
import com.example.attenbot.data.remote.model.ResetAttendanceResponse
import com.example.attenbot.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SyncResult {
    data class Success(
        val insertedCount: Int,
        val skippedCount: Int,
        val totalFetched: Int,
        val downloadedRecords: List<AttendanceRecord> = emptyList()
    ) : SyncResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : SyncResult()
}

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val networkClient: Esp32NetworkClient = Esp32NetworkClient(),
    private val csvParser: CsvParser = CsvParser()
) {
    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()

    fun searchRecords(query: String): Flow<List<AttendanceRecord>> {
        return if (query.isBlank()) {
            attendanceDao.getAllRecords()
        } else {
            attendanceDao.searchRecords(query.trim())
        }
    }

    suspend fun getRecordsForExport(): List<AttendanceRecord> {
        return attendanceDao.getAllRecordsForExport()
    }

    suspend fun getStatus(baseUrl: String): NetworkResult<Esp32Status> {
        return networkClient.getStatus(baseUrl)
    }

    suspend fun getAttendance(baseUrl: String): NetworkResult<List<ApiAttendanceRecord>> {
        val result = networkClient.getAttendance(baseUrl)
        if (result is NetworkResult.Success) {
            val apiRecords = result.data
            val todayDate = getCurrentFormattedDate()
            val roomRecords = apiRecords.map { apiRec ->
                AttendanceRecord(
                    date = if (apiRec.timestamp.length >= 10) apiRec.timestamp.substring(0, 10) else todayDate,
                    rollNumber = apiRec.rollNumber,
                    studentName = apiRec.name,
                    deviceId = apiRec.deviceId.ifBlank { "CLASSROOM_A" },
                    timestamp = apiRec.timestamp
                )
            }
            if (roomRecords.isNotEmpty()) {
                attendanceDao.insertRecords(roomRecords)
            }
        }
        return result
    }

    suspend fun downloadCsvStreamToUri(
        baseUrl: String,
        pin: String,
        destinationUri: Uri,
        contentResolver: ContentResolver
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        when (val streamResult = networkClient.downloadCsvStream(baseUrl, pin)) {
            is NetworkResult.Error -> NetworkResult.Error(streamResult.message, streamResult.cause)
            is NetworkResult.Success -> {
                val body = streamResult.data
                try {
                    contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        body.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: return@withContext NetworkResult.Error("Failed to open output stream for selected document URI.")
                    NetworkResult.Success(Unit)
                } catch (e: Exception) {
                    NetworkResult.Error("Failed writing CSV stream: ${e.localizedMessage ?: "File write error"}", e)
                }
            }
        }
    }

    suspend fun resetAttendance(baseUrl: String, pin: String): NetworkResult<ResetAttendanceResponse> {
        return networkClient.resetAttendance(baseUrl, pin)
    }

    suspend fun setRtcTime(baseUrl: String, pin: String, datetime: String): NetworkResult<ResetAttendanceResponse> {
        return networkClient.setRtcTime(baseUrl, pin, datetime)
    }

    suspend fun syncWithEsp32(
        url: String = "http://192.168.4.1/download",
        overrideDate: String? = null
    ): SyncResult {
        val targetUrl = UrlUtils.normalizeBaseUrl(url) + "download"
        return when (val networkResult = networkClient.fetchAttendanceCsv(targetUrl)) {
            is NetworkResult.Error -> {
                SyncResult.Error(networkResult.message, networkResult.cause)
            }
            is NetworkResult.Success -> {
                val csvData = networkResult.data
                val parsedStudents = csvParser.parse(csvData)

                if (parsedStudents.isEmpty()) {
                    return SyncResult.Success(
                        insertedCount = 0,
                        skippedCount = 0,
                        totalFetched = 0,
                        downloadedRecords = emptyList()
                    )
                }

                val todayDate = overrideDate ?: getCurrentFormattedDate()

                val recordsToInsert = parsedStudents.map { student ->
                    AttendanceRecord(
                        date = todayDate,
                        rollNumber = student.rollNumber,
                        studentName = student.studentName
                    )
                }

                val insertResults = attendanceDao.insertRecords(recordsToInsert)
                val insertedCount = insertResults.count { it != -1L }
                val skippedCount = insertResults.count { it == -1L }

                SyncResult.Success(
                    insertedCount = insertedCount,
                    skippedCount = skippedCount,
                    totalFetched = parsedStudents.size,
                    downloadedRecords = recordsToInsert
                )
            }
        }
    }

    private fun getCurrentFormattedDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateFormat.format(Date())
    }
}
