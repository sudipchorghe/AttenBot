package com.example.attenbot

import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.remote.Esp32NetworkClient
import com.example.attenbot.data.remote.NetworkResult
import com.example.attenbot.data.repository.AttendanceRepository
import com.example.attenbot.data.repository.SyncResult
import com.example.attenbot.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockDao: FakeAttendanceDao
    private lateinit var mockNetworkClient: FakeNetworkClient
    private lateinit var repository: AttendanceRepository
    private lateinit var viewModel: AttendanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDao = FakeAttendanceDao()
        mockNetworkClient = FakeNetworkClient()
        repository = AttendanceRepository(mockDao, mockNetworkClient)
        viewModel = AttendanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiStates() {
        val syncState = viewModel.syncUiState.value
        assertEquals("http://192.168.4.1/download", syncState.esp32Url)
        assertFalse(syncState.isSyncing)
        assertEquals("Ready to Sync", syncState.statusText)

        val exportState = viewModel.exportUiState.value
        assertFalse(exportState.isExporting)
        assertEquals(0, exportState.totalRecordsToExport)
    }

    @Test
    fun testUpdateEsp32Url() {
        viewModel.updateEsp32Url("http://192.168.1.100/download")
        assertEquals("http://192.168.1.100/download", viewModel.syncUiState.value.esp32Url)
    }

    @Test
    fun testSyncAttendance_Success() = runTest(testDispatcher) {
        mockNetworkClient.responseToReturn = NetworkResult.Success("101, Alice\n102, Bob")

        viewModel.syncAttendance(overrideDate = "2026-03-31")
        testDispatcher.scheduler.advanceUntilIdle()

        val syncState = viewModel.syncUiState.value
        assertFalse(syncState.isSyncing)
        assertTrue(syncState.isConnected)
        assertEquals("Success", syncState.statusText)
        assertTrue(syncState.syncResult is SyncResult.Success)

        val successResult = syncState.syncResult as SyncResult.Success
        assertEquals(2, successResult.insertedCount)
        assertEquals(2, successResult.totalFetched)
        assertEquals(2, syncState.activeSessionRecords.size)
        assertEquals("Alice", syncState.activeSessionRecords[0].studentName)
        assertEquals("101", syncState.activeSessionRecords[0].rollNumber)
    }

    @Test
    fun testSyncAttendance_Failure() = runTest(testDispatcher) {
        mockNetworkClient.responseToReturn = NetworkResult.Error("Unable to reach Attendance_Bot AP")

        viewModel.syncAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        val syncState = viewModel.syncUiState.value
        assertFalse(syncState.isSyncing)
        assertFalse(syncState.isConnected)
        assertEquals("Connection Error", syncState.statusText)
        assertTrue(syncState.syncResult is SyncResult.Error)
    }

    @Test
    fun testUpdateSearchQuery() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch { viewModel.historyUiState.collect() }

        viewModel.updateSearchQuery("Alice")
        testDispatcher.scheduler.advanceUntilIdle()

        val historyState = viewModel.historyUiState.value
        assertEquals("Alice", historyState.searchQuery)

        collectJob.cancel()
    }

    private class FakeAttendanceDao : AttendanceDao {
        private val records = mutableListOf<AttendanceRecord>()
        private val flow = MutableStateFlow<List<AttendanceRecord>>(emptyList())

        override suspend fun insertRecords(records: List<AttendanceRecord>): List<Long> {
            val insertedIds = mutableListOf<Long>()
            for (r in records) {
                if (this.records.any { it.date == r.date && it.rollNumber == r.rollNumber }) {
                    insertedIds.add(-1L)
                } else {
                    val newRecord = r.copy(id = this.records.size + 1)
                    this.records.add(newRecord)
                    insertedIds.add(newRecord.id.toLong())
                }
            }
            flow.value = this.records.toList()
            return insertedIds
        }

        override suspend fun insertRecord(record: AttendanceRecord): Long {
            val newRecord = record.copy(id = this.records.size + 1)
            this.records.add(newRecord)
            flow.value = this.records.toList()
            return newRecord.id.toLong()
        }

        override fun getAllRecords(): Flow<List<AttendanceRecord>> = flow

        override fun searchRecords(query: String): Flow<List<AttendanceRecord>> {
            val filtered = records.filter {
                it.rollNumber.contains(query, ignoreCase = true) ||
                        it.studentName.contains(query, ignoreCase = true) ||
                        it.date.contains(query, ignoreCase = true)
            }
            return MutableStateFlow(filtered)
        }

        override suspend fun getAllRecordsForExport(): List<AttendanceRecord> = records.toList()

        override suspend fun deleteAllRecords() {
            records.clear()
            flow.value = emptyList()
        }
    }

    private class FakeNetworkClient : Esp32NetworkClient() {
        var responseToReturn: NetworkResult<String> = NetworkResult.Success("")

        override suspend fun fetchAttendanceCsv(url: String): NetworkResult<String> {
            return responseToReturn
        }
    }
}
