package com.example.attenbot

import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.remote.Esp32NetworkClient
import com.example.attenbot.data.remote.NetworkResult
import com.example.attenbot.data.remote.model.ApiAttendanceRecord
import com.example.attenbot.data.remote.model.Esp32Status
import com.example.attenbot.data.remote.model.ResetAttendanceResponse
import com.example.attenbot.data.repository.AttendanceRepository
import com.example.attenbot.data.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAttendanceDao : AttendanceDao {
    val records = mutableListOf<AttendanceRecord>()
    private var idCounter = 1

    override suspend fun insertRecords(recordsToInsert: List<AttendanceRecord>): List<Long> {
        val results = mutableListOf<Long>()
        for (rec in recordsToInsert) {
            val exists = records.any { it.date == rec.date && it.rollNumber == rec.rollNumber }
            if (exists) {
                results.add(-1L)
            } else {
                val newRecord = rec.copy(id = idCounter++)
                records.add(newRecord)
                results.add(newRecord.id.toLong())
            }
        }
        return results
    }

    override suspend fun insertRecord(record: AttendanceRecord): Long {
        val exists = records.any { it.date == record.date && it.rollNumber == record.rollNumber }
        return if (exists) {
            -1L
        } else {
            val newRecord = record.copy(id = idCounter++)
            records.add(newRecord)
            newRecord.id.toLong()
        }
    }

    override fun getAllRecords(): Flow<List<AttendanceRecord>> {
        return flowOf(records)
    }

    override fun searchRecords(query: String): Flow<List<AttendanceRecord>> {
        val filtered = records.filter {
            it.rollNumber.contains(query, ignoreCase = true) ||
            it.studentName.contains(query, ignoreCase = true) ||
            it.date.contains(query, ignoreCase = true)
        }
        return flowOf(filtered)
    }

    override suspend fun getAllRecordsForExport(): List<AttendanceRecord> {
        return records.toList()
    }

    override suspend fun deleteAllRecords() {
        records.clear()
    }
}

class FakeEsp32NetworkClient : Esp32NetworkClient() {
    var statusResult: NetworkResult<Esp32Status> = NetworkResult.Success(Esp32Status("CLASSROOM_A", false, 5))
    var attendanceResult: NetworkResult<List<ApiAttendanceRecord>> = NetworkResult.Success(emptyList())
    var downloadResult: NetworkResult<okhttp3.ResponseBody> = NetworkResult.Success("".toResponseBody(null))
    var resetResult: NetworkResult<ResetAttendanceResponse> = NetworkResult.Success(ResetAttendanceResponse(true, "Attendance cleared"))
    var rtcResult: NetworkResult<ResetAttendanceResponse> = NetworkResult.Success(ResetAttendanceResponse(true, "RTC time updated"))
    var csvResult: NetworkResult<String> = NetworkResult.Success("")

    override suspend fun getStatus(baseUrl: String): NetworkResult<Esp32Status> = statusResult
    override suspend fun getAttendance(baseUrl: String): NetworkResult<List<ApiAttendanceRecord>> = attendanceResult
    override suspend fun downloadCsvStream(baseUrl: String, pin: String): NetworkResult<okhttp3.ResponseBody> = downloadResult
    override suspend fun resetAttendance(baseUrl: String, pin: String): NetworkResult<ResetAttendanceResponse> = resetResult
    override suspend fun setRtcTime(baseUrl: String, pin: String, datetime: String): NetworkResult<ResetAttendanceResponse> = rtcResult
    override suspend fun fetchAttendanceCsv(url: String): NetworkResult<String> = csvResult
}

class AttendanceRepositoryTest {

    private lateinit var fakeDao: FakeAttendanceDao
    private lateinit var fakeNetworkClient: FakeEsp32NetworkClient
    private lateinit var repository: AttendanceRepository

    @Before
    fun setUp() {
        fakeDao = FakeAttendanceDao()
        fakeNetworkClient = FakeEsp32NetworkClient()
        repository = AttendanceRepository(fakeDao, fakeNetworkClient)
    }

    @Test
    fun testGetStatus_Success() = runBlocking {
        fakeNetworkClient.statusResult = NetworkResult.Success(Esp32Status("CLASSROOM_A", false, 12))
        val result = repository.getStatus("http://192.168.4.1/")
        assertTrue(result is NetworkResult.Success)
        val status = (result as NetworkResult.Success).data
        assertEquals("CLASSROOM_A", status.deviceId)
        assertFalse(status.closed)
        assertEquals(12, status.count)
    }

    @Test
    fun testGetAttendance_SuccessAndInsertsToRoom() = runBlocking {
        val records = listOf(
            ApiAttendanceRecord("CLASSROOM_A", "John Doe", "101", "2026-10-15 09:42:10")
        )
        fakeNetworkClient.attendanceResult = NetworkResult.Success(records)

        val result = repository.getAttendance("http://192.168.4.1/")
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, fakeDao.records.size)
        assertEquals("John Doe", fakeDao.records[0].studentName)
        assertEquals("101", fakeDao.records[0].rollNumber)
    }

    @Test
    fun testResetAttendance_Success() = runBlocking {
        fakeNetworkClient.resetResult = NetworkResult.Success(ResetAttendanceResponse(true, "Attendance cleared for the next lecture"))
        val result = repository.resetAttendance("http://192.168.4.1/", "2468")
        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).data.success)
    }

    @Test
    fun testResetAttendance_InvalidPin() = runBlocking {
        fakeNetworkClient.resetResult = NetworkResult.Error("Invalid teacher PIN.")
        val result = repository.resetAttendance("http://192.168.4.1/", "wrong")
        assertTrue(result is NetworkResult.Error)
        assertEquals("Invalid teacher PIN.", (result as NetworkResult.Error).message)
    }

    @Test
    fun testSetRtcTime_Success() = runBlocking {
        fakeNetworkClient.rtcResult = NetworkResult.Success(ResetAttendanceResponse(true, "RTC time updated"))
        val result = repository.setRtcTime("http://192.168.4.1/", "2468", "2026-10-15 10:00:00")
        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).data.success)
    }

    @Test
    fun syncWithEsp32SuccessAndIgnoresDuplicates() = runBlocking {
        val csvContent = """
            101,Rahul Sharma
            102,Priya Patel
        """.trimIndent()

        fakeNetworkClient.csvResult = NetworkResult.Success(csvContent)

        val result1 = repository.syncWithEsp32(overrideDate = "2025-03-01")
        assertTrue(result1 is SyncResult.Success)
        val success1 = result1 as SyncResult.Success
        assertEquals(2, success1.insertedCount)
        assertEquals(0, success1.skippedCount)
        assertEquals(2, success1.totalFetched)
    }
}
