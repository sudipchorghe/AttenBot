@file:Suppress("unused")

package com.example.attenbot.data.remote

sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class InvalidUrl(override val message: String = "Invalid ESP32 base URL.") : AppError(message)
    data class DeviceUnavailable(override val message: String = "Unable to reach ESP32. Ensure phone is connected to ESP32 Wi-Fi.") : AppError(message)
    data class Timeout(override val message: String = "Connection timed out while trying to reach ESP32.") : AppError(message)
    data class Unauthorized(override val message: String = "Invalid teacher PIN.") : AppError(message)
    data class NotFound(override val message: String = "Endpoint not found on ESP32 (HTTP 404).") : AppError(message)
    data class ServerError(override val message: String = "ESP32 server error (HTTP 500).") : AppError(message)
    data class InvalidResponse(override val message: String = "Received invalid or malformed JSON from ESP32.") : AppError(message)
    data class FileWriteFailed(override val message: String = "Failed writing CSV to local file storage.") : AppError(message)
    data class UnknownError(
        override val message: String = "An unexpected network error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
