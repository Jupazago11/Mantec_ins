package com.example.mantec_ins.data.remote.personal

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// Consume Api\Personal\* del backend Laravel (rutas api/personal/...,
// seccion "API Supervisor" 2026-09-20). Separado de AuthApiService/
// SyncApiService (Inspector) porque autentica con un token de Employee,
// no de User. El login unico vive en AuthApiService/AuthRepository (ver
// LOGIN_Y_ROLES.md) — esta interfaz solo se usa para el logout remoto de
// una sesion de Supervisor y para los datos de actividades/evidencias.
interface PersonalApiService {

    @POST("api/personal/login")
    suspend fun login(@Body request: PersonalLoginRequest): PersonalLoginResponse

    @POST("api/personal/logout")
    suspend fun logout(): PersonalLogoutResponse

    @GET("api/personal/actividades")
    suspend fun getActividades(@Query("date") date: String? = null): PersonalActivitiesResponse

    // Response<T> (no el DTO directo) — a diferencia de login/getActividades,
    // estos dos alimentan SupervisorSyncRepository, que necesita el codigo
    // HTTP exacto para distinguir fallo transitorio de fallo permanente
    // (403/422/413), mismo criterio que SyncApiService del Inspector.
    @POST("api/personal/actividades/{id}")
    suspend fun saveActividad(
        @Path("id") activityId: Long,
        @Body request: PersonalSaveActivityRequest
    ): Response<PersonalSaveActivityResponse>

    @Multipart
    @POST("api/personal/actividades/{id}/evidencias")
    suspend fun uploadEvidencias(
        @Path("id") activityId: Long,
        @Part files: List<MultipartBody.Part>
    ): Response<PersonalEvidenceUploadResponse>

    @GET("api/personal/actividades/{activityId}/evidencias/{evidenceId}")
    suspend fun showEvidencia(
        @Path("activityId") activityId: Long,
        @Path("evidenceId") evidenceId: Long
    ): PersonalEvidenceShowResponse

    @DELETE("api/personal/actividades/{activityId}/evidencias/{evidenceId}")
    suspend fun deleteEvidencia(
        @Path("activityId") activityId: Long,
        @Path("evidenceId") evidenceId: Long
    ): PersonalGenericResponse
}
