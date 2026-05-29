package com.example.mantec_ins.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApiService {

    @POST("api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/inspector/elements/{elementId}/pending-diagnostics")
    suspend fun getPendingDiagnostics(
        @Path("elementId") elementId: Long
    ): List<RemotePendingDiagnosticDto>

    @GET("api/inspector/clients/{clientId}/areas")
    suspend fun getAreasByClient(
        @Path("clientId") clientId: Long
    ): List<RemoteAreaDto>



    @GET("api/inspector/elements/{elementId}/conditions")
    suspend fun getConditionsByElement(
        @Path("elementId") elementId: Long,
        @Query("component_id") componentId: Long
    ): List<RemoteConditionDto>

    @GET("api/inspector/areas/{areaId}/elements")
    suspend fun getElementsByArea(
        @Path("areaId") areaId: Long
    ): List<RemoteElementDto>

    @GET("api/inspector/elements/{elementId}/components")
    suspend fun getComponentsByElement(
        @Path("elementId") elementId: Long
    ): List<RemoteComponentDto>

    @GET("api/inspector/elements/{elementId}/weekly-diagnostic-status")
    suspend fun getWeeklyDiagnosticStatus(
        @Path("elementId") elementId: Long
    ): List<RemoteWeeklyDiagnosticStatusDto>

    @GET("api/inspector/components/{componentId}/diagnostics")
    suspend fun getDiagnosticsByComponent(
        @Path("componentId") componentId: Long,
        @Query("element_id") elementId: Long
    ): List<RemoteDiagnosticDto>

    @GET("api/inspector/areas/{areaId}/weekly-elements-status")
    suspend fun getWeeklyElementsStatus(
        @Path("areaId") areaId: Long,
        @Query("element_type_id") elementTypeId: Long
    ): List<RemoteWeeklyElementStatusDto>

    @GET("api/inspector/offline-catalog")
    suspend fun getOfflineCatalog(): OfflineCatalogResponse

}
