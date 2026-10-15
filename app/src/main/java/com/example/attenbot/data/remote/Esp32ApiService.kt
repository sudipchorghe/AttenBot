package com.example.attenbot.data.remote

import com.example.attenbot.data.remote.model.ApiAttendanceRecord
import com.example.attenbot.data.remote.model.Esp32Status
import com.example.attenbot.data.remote.model.ResetAttendanceResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Esp32ApiService {

    @GET("api/status")
    suspend fun getStatus(): Response<Esp32Status>

    @GET("api/attendance")
    suspend fun getAttendance(): Response<List<ApiAttendanceRecord>>

    @GET("download")
    suspend fun downloadCsv(
        @Query("pin") pin: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("api/reset")
    suspend fun resetAttendance(
        @Field("pin") pin: String
    ): Response<ResetAttendanceResponse>

    @FormUrlEncoded
    @POST("api/set-time")
    suspend fun setRtcTime(
        @Field("pin") pin: String,
        @Field("datetime") datetime: String
    ): Response<ResetAttendanceResponse>
}
