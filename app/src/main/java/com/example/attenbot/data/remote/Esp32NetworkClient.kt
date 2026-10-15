package com.example.attenbot.data.remote

import com.example.attenbot.data.remote.model.ApiAttendanceRecord
import com.example.attenbot.data.remote.model.Esp32Status
import com.example.attenbot.data.remote.model.ResetAttendanceResponse
import com.example.attenbot.util.UrlUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun getApiService(baseUrl: String): Esp32ApiService {
        val normalizedUrl = UrlUtils.normalizeBaseUrl(baseUrl)
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(Esp32ApiService::class.java)
    }

    open suspend fun getStatus(baseUrl: String): NetworkResult<Esp32Status> = safeApiCall {
        getApiService(baseUrl).getStatus()
    }

    open suspend fun getAttendance(baseUrl: String): NetworkResult<List<ApiAttendanceRecord>> = safeApiCall {
        getApiService(baseUrl).getAttendance()
    }

    open suspend fun downloadCsvStream(baseUrl: String, pin: String): NetworkResult<ResponseBody> = safeApiCall {
        getApiService(baseUrl).downloadCsv(pin)
    }

    open suspend fun resetAttendance(baseUrl: String, pin: String): NetworkResult<ResetAttendanceResponse> = safeApiCall {
        getApiService(baseUrl).resetAttendance(pin)
    }

    open suspend fun setRtcTime(baseUrl: String, pin: String, datetime: String): NetworkResult<ResetAttendanceResponse> = safeApiCall {
        getApiService(baseUrl).setRtcTime(pin, datetime)
    }

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

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        NetworkResult.Success(body)
                    } else {
                        NetworkResult.Error("Received empty response body from ESP32")
                    }
                } else {
                    val code = response.code()
                    val msg = when (code) {
                        401, 403 -> "Invalid teacher PIN."
                        404 -> "Endpoint not found on ESP32 (HTTP 404)."
                        500 -> "ESP32 server error (HTTP 500)."
                        else -> "HTTP $code: ${response.message()}"
                    }
                    NetworkResult.Error(msg)
                }
            } catch (e: UnknownHostException) {
                NetworkResult.Error("Unable to reach ESP32. Please connect to ESP32 Wi-Fi network.", e)
            } catch (e: ConnectException) {
                NetworkResult.Error("Connection refused by ESP32. Check IP address and Wi-Fi connection.", e)
            } catch (e: SocketTimeoutException) {
                NetworkResult.Error("Connection timed out while reaching ESP32.", e)
            } catch (e: UnresolvedAddressException) {
                NetworkResult.Error("Unresolved address. Check ESP32 Wi-Fi connection.", e)
            } catch (e: com.squareup.moshi.JsonDataException) {
                NetworkResult.Error("Invalid or malformed JSON received from ESP32.", e)
            } catch (e: IOException) {
                NetworkResult.Error("Network error: ${e.localizedMessage ?: "Communication failed"}", e)
            } catch (e: Exception) {
                NetworkResult.Error("Unexpected error: ${e.localizedMessage ?: "Unknown failure"}", e)
            }
        }
    }
}
