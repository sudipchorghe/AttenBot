package com.example.attenbot.data.repository

import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.parser.CsvParser
import com.example.attenbot.data.remote.Esp32NetworkClient
import com.example.attenbot.data.remote.NetworkResult
import kotlinx.coroutines.flow.Flow
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

    suspend fun syncWithEsp32(
        url: String = "http://192.168.4.1/download",
        overrideDate: String? = null
    ): SyncResult {
        return when (val networkResult = networkClient.fetchAttendanceCsv(url)) {
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
