package com.example.mantec_ins.data.repository

import android.util.Log
import com.example.mantec_ins.data.remote.personal.PersonalApiService

// Acciones ONLINE-ONLY del supervisor sobre evidencia ya sincronizada:
// ver la URL firmada de R2 y borrarla del servidor. No participan del
// patron offline PENDING_SYNC (mismo criterio que el Inspector: no hay
// "ver"/"borrar" offline para reportes ya sincronizados tampoco). Leer/
// guardar actividades y encolar/subir evidencia nueva ahora vive en
// PersonalActivityLocalRepository (Room) + SupervisorSyncRepository
// (push/pull real) — ver OFFLINE_SUPERVISOR.md.
class PersonalActivityRepository(
    private val api: PersonalApiService
) {

    suspend fun obtenerUrlEvidencia(activityId: Long, evidenceId: Long): Result<String> {
        return try {
            val response = api.showEvidencia(activityId, evidenceId)
            if (response.success) Result.success(response.url) else Result.failure(Exception("No se pudo obtener la evidencia."))
        } catch (e: Exception) {
            Log.e("PERSONAL_ACTIVITY_REPO", "Error obteniendo url de evidencia=$evidenceId", e)
            Result.failure(e)
        }
    }

    suspend fun borrarEvidenciaRemota(activityId: Long, evidenceId: Long): Result<Unit> {
        return try {
            val response = api.deleteEvidencia(activityId, evidenceId)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.message))
        } catch (e: Exception) {
            Log.e("PERSONAL_ACTIVITY_REPO", "Error borrando evidencia=$evidenceId", e)
            Result.failure(e)
        }
    }
}
