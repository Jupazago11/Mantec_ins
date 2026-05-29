package com.example.mantec_ins.data.remote

import com.example.mantec_ins.sync.SyncReportRequest
import com.example.mantec_ins.sync.SyncReportResponse
import com.example.mantec_ins.sync.UploadFileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface SyncApiService {

    @POST("api/inspector/reports/sync")
    suspend fun syncReport(
        @Body request: SyncReportRequest
    ): Response<SyncReportResponse>

    @Multipart
    @POST("api/inspector/report-details/{id}/files")
    suspend fun uploadReportFile(
        @Path("id") reportDetailId: Long,
        @Part file: MultipartBody.Part,
        @Part("sort_order") sortOrder: RequestBody
    ): Response<UploadFileResponse>
}
