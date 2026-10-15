package com.example.attenbot.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Esp32Status(
    @field:Json(name = "device_id")
    val deviceId: String = "CLASSROOM_A",
    @field:Json(name = "closed")
    val closed: Boolean = false,
    @field:Json(name = "count")
    val count: Int = 0
)
