package com.example.attenbot.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResetAttendanceResponse(
    @field:Json(name = "success")
    val success: Boolean = false,
    @field:Json(name = "message")
    val message: String = ""
)
