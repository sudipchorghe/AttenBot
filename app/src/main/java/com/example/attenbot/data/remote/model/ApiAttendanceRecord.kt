package com.example.attenbot.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiAttendanceRecord(
    @field:Json(name = "device_id")
    val deviceId: String = "",
    @field:Json(name = "name")
    val name: String = "",
    @field:Json(name = "roll_number")
    val rollNumber: String = "",
    @field:Json(name = "timestamp")
    val timestamp: String = "",
)
