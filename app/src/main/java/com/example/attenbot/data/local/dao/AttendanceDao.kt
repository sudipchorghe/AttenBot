package com.example.attenbot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.attenbot.data.local.entity.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<AttendanceRecord>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, id DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query(
        """
        SELECT * FROM attendance_records 
        WHERE rollNumber LIKE '%' || :query || '%' 
           OR studentName LIKE '%' || :query || '%' 
           OR date LIKE '%' || :query || '%' 
        ORDER BY date DESC, id DESC
        """
    )
    fun searchRecords(query: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, id DESC")
    suspend fun getAllRecordsForExport(): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()
}
