package com.example.attenbot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["date", "rollNumber"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val rollNumber: String,
    val studentName: String,
    val deviceId: String = "CLASSROOM_A",
    val timestamp: String = ""
)
