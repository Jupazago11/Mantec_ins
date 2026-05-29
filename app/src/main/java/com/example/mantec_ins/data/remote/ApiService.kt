package com.example.mantec_ins.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("api/catalog/version")
    suspend fun getCatalogVersion(): CatalogVersionResponse

    @GET("inspector/elements/{elementId}/pending-diagnostics")
    suspend fun getPendingDiagnostics(
        @Path("elementId") elementId: Long
    ): List<RemotePendingDiagnosticDto>
}