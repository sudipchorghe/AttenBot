package com.example.attenbot.data.remote

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>()
}
