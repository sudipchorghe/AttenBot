package com.example.attenbot.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeUnit

open class Esp32NetworkClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    open suspend fun fetchAttendanceCsv(url: String = "http://192.168.4.1/download"): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext NetworkResult.Error("HTTP Error ${response.code}: ${response.message}")
                    }
                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrEmpty()) {
                        return@withContext NetworkResult.Error("Received empty response from ESP32")
                    }
                    NetworkResult.Success(bodyString)
                }
            } catch (e: UnknownHostException) {
                NetworkResult.Error("Unable to reach Attendance_Bot AP. Please connect to the ESP32 Wi-Fi network (http://192.168.4.1).", e)
            } catch (e: ConnectException) {
                NetworkResult.Error("Connection refused by ESP32. Ensure you are connected to Attendance_Bot Wi-Fi AP.", e)
            } catch (e: SocketTimeoutException) {
                NetworkResult.Error("Connection timed out while trying to reach ESP32 at $url.", e)
            } catch (e: UnresolvedAddressException) {
                NetworkResult.Error("Unresolved address. Ensure you are connected to the ESP32 Wi-Fi network.", e)
            } catch (e: IOException) {
                NetworkResult.Error("Network error: ${e.localizedMessage ?: "Failed to download attendance data"}", e)
            } catch (e: Exception) {
                NetworkResult.Error("Unexpected error during sync: ${e.localizedMessage}", e)
            }
        }
    }
}
