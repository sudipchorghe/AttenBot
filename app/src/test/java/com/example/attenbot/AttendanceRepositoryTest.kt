package com.example.attenbot

import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.remote.Esp32NetworkClient
import com.example.attenbot.data.remote.NetworkResult
import com.example.attenbot.data.repository.AttendanceRepository
import com.example.attenbot.data.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

class FakeEsp32NetworkClient(
    private var resultToReturn: NetworkResult<String>
) : Esp32NetworkClient() {

    fun setResult(result: NetworkResult<String>) {
        resultToReturn = result
    }

    override suspend fun fetchAttendanceCsv(url: String): NetworkResult<String> {
        return resultToReturn
    }
}

class AttendanceRepositoryTest {

    private lateinit var fakeDao: FakeAttendanceDao
    private lateinit var fakeNetworkClient: FakeEsp32NetworkClient
    private lateinit var repository: AttendanceRepository

    @Before
    fun setUp() {
        fakeDao = FakeAttendanceDao()
        fakeNetworkClient = FakeEsp32NetworkClient(NetworkResult.Success(""))
        repository = AttendanceRepository(fakeDao, fakeNetworkClient)
    }

    @Test
    fun syncWithEsp32SuccessAndIgnoresDuplicates() = runBlocking {
        val csvContent = """
            101,Rahul Sharma
            102,Priya Patel
        """.trimIndent()

        fakeNetworkClient.setResult(NetworkResult.Success(csvContent))

        // First sync for 2025-03-01
        val result1 = repository.syncWithEsp32(overrideDate = "2025-03-01")
        assertTrue(result1 is SyncResult.Success)
        val success1 = result1 as SyncResult.Success
        assertEquals(2, success1.insertedCount)
        assertEquals(0, success1.skippedCount)
        assertEquals(2, success1.totalFetched)

        // Second sync with same date and same records should skip
        val result2 = repository.syncWithEsp32(overrideDate = "2025-03-01")
        assertTrue(result2 is SyncResult.Success)
        val success2 = result2 as SyncResult.Success
        assertEquals(0, success2.insertedCount)
        assertEquals(2, success2.skippedCount)
        assertEquals(2, success2.totalFetched)
    }

    @Test
    fun syncWithEsp32NetworkError() = runBlocking {
        fakeNetworkClient.setResult(NetworkResult.Error("Connection refused by ESP32"))

        val result = repository.syncWithEsp32()
        assertTrue(result is SyncResult.Error)
        assertEquals("Connection refused by ESP32", (result as SyncResult.Error).message)
    }
}
