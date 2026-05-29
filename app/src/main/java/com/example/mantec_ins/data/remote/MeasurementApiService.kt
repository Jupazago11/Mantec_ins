package com.example.mantec_ins.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MeasurementApiService {

    @GET("api/inspector/measurements/element-types")
    suspend fun getMeasurementElementTypes(): MeasurementElementTypesResponse

    @GET("api/inspector/measurements/element-types/{elementTypeId}/areas")
    suspend fun getAreasByElementType(
        @Path("elementTypeId") elementTypeId: Long
    ): MeasurementAreasResponse

    @GET("api/inspector/measurements/areas/{areaId}/element-types/{elementTypeId}/elements")
    suspend fun getElementsByAreaAndElementType(
        @Path("areaId") areaId: Long,
        @Path("elementTypeId") elementTypeId: Long
    ): MeasurementElementsResponse

    @GET("api/inspector/measurements/elements/{elementId}/thickness")
    suspend fun getThicknessState(
        @Path("elementId") elementId: Long
    ): MeasurementThicknessStateResponse

    @POST("api/inspector/measurements/elements/{elementId}/thickness/draft/sync")
    suspend fun syncThicknessDraft(
        @Path("elementId") elementId: Long,
        @Body request: MeasurementThicknessDraftSyncRequest
    ): Response<MeasurementThicknessDraftSyncResponse>
}