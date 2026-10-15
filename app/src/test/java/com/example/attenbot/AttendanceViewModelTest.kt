package com.example.attenbot

import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord
import com.example.attenbot.data.remote.Esp32NetworkClient
import com.example.attenbot.data.remote.NetworkResult
import com.example.attenbot.data.remote.model.ApiAttendanceRecord
import com.example.attenbot.data.remote.model.Esp32Status
import com.example.attenbot.data.remote.model.ResetAttendanceResponse
import com.example.attenbot.data.repository.AttendanceRepository
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
import org.junit.Assert.assertNull
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
        assertEquals("http://192.168.4.1/", syncState.baseUrl)
        assertFalse(syncState.isConnecting)
        assertFalse(syncState.isResetting)
        assertEquals("Ready", syncState.statusText)
    }

    @Test
    fun testUpdateEsp32Url_normalizesUrl() {
        viewModel.updateEsp32Url("192.168.4.2/download")
        assertEquals("http://192.168.4.2/", viewModel.syncUiState.value.baseUrl)
    }

    @Test
    fun testRefreshStatus_Success() = runTest(testDispatcher) {
        mockNetworkClient.statusResult = NetworkResult.Success(Esp32Status("CLASSROOM_B", false, 15))

        viewModel.refreshStatus()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.syncUiState.value
        assertFalse(state.isConnecting)
        assertTrue(state.isConnected)
        assertEquals("CLASSROOM_B", state.deviceId)
        assertEquals(15, state.count)
        assertFalse(state.closed)
        assertEquals("Attendance OPEN", state.statusText)
    }

    @Test
    fun testRefreshAttendance_Success() = runTest(testDispatcher) {
        val recs = listOf(
            ApiAttendanceRecord("CLASSROOM_A", "Alice Smith", "101", "2026-10-15 09:42:10")
        )
        mockNetworkClient.attendanceResult = NetworkResult.Success(recs)

        viewModel.refreshAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.syncUiState.value
        assertFalse(state.isRefreshing)
        assertTrue(state.isConnected)
        assertEquals(1, state.liveRecords.size)
        assertEquals("Alice Smith", state.liveRecords[0].name)
    }

    @Test
    fun testClearAttendanceForNextLecture_Success() = runTest(testDispatcher) {
        mockNetworkClient.resetResult = NetworkResult.Success(ResetAttendanceResponse(true, "Attendance cleared"))
        mockNetworkClient.statusResult = NetworkResult.Success(Esp32Status("CLASSROOM_A", false, 0))
        mockNetworkClient.attendanceResult = NetworkResult.Success(emptyList())

        viewModel.clearAttendanceForNextLecture("2468")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.syncUiState.value
        assertFalse(state.isResetting)
        assertEquals(0, state.count)
        assertTrue(state.liveRecords.isEmpty())
        assertEquals("Attendance cleared. The ESP32 is ready for the next lecture.", state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun testClearAttendanceForNextLecture_BlankPin_ShowsError() = runTest(testDispatcher) {
        viewModel.clearAttendanceForNextLecture("")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.syncUiState.value
        assertEquals("Invalid teacher PIN.", state.errorMessage)
    }

    @Test
    fun testSetRtcTime_Success() = runTest(testDispatcher) {
        mockNetworkClient.rtcResult = NetworkResult.Success(ResetAttendanceResponse(true, "RTC time updated"))

        viewModel.setRtcTime("2468", "2026-10-15 10:00:00")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.syncUiState.value
        assertFalse(state.isSettingRtcTime)
        assertEquals("RTC time updated successfully.", state.successMessage)
    }

    @Test
    fun testSetRtcTime_InvalidFormat_ShowsError() = runTest(testDispatcher) {
        viewModel.setRtcTime("2468", "invalid-datetime")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.syncUiState.value
        assertEquals("Invalid date/time format. Use YYYY-MM-DD HH:MM:SS.", state.errorMessage)
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
        var statusResult: NetworkResult<Esp32Status> = NetworkResult.Success(Esp32Status("CLASSROOM_A", false, 0))
        var attendanceResult: NetworkResult<List<ApiAttendanceRecord>> = NetworkResult.Success(emptyList())
        var resetResult: NetworkResult<ResetAttendanceResponse> = NetworkResult.Success(ResetAttendanceResponse(true, "Success"))
        var rtcResult: NetworkResult<ResetAttendanceResponse> = NetworkResult.Success(ResetAttendanceResponse(true, "Success"))

        override suspend fun getStatus(baseUrl: String): NetworkResult<Esp32Status> = statusResult
        override suspend fun getAttendance(baseUrl: String): NetworkResult<List<ApiAttendanceRecord>> = attendanceResult
        override suspend fun resetAttendance(baseUrl: String, pin: String): NetworkResult<ResetAttendanceResponse> = resetResult
        override suspend fun setRtcTime(baseUrl: String, pin: String, datetime: String): NetworkResult<ResetAttendanceResponse> = rtcResult
    }
}
