package com.example.mantec_ins.data.repository

import android.util.Log
import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.SupervisorActivityEntity
import com.example.mantec_ins.data.local.SupervisorEvidenceEntity
import com.example.mantec_ins.data.local.SupervisorPersonaEntity
import com.example.mantec_ins.data.remote.personal.PersonalApiService
import com.example.mantec_ins.data.remote.personal.PersonalPersonaHoursRequest
import com.example.mantec_ins.data.remote.personal.PersonalSaveActivityRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

// Calco de SyncRepository (Inspector) para el modulo Supervisor: push de
// lo pendiente (registro de horas/comentarios + evidencia), luego
// refresh/merge desde el servidor. Mismo Mutex para evitar sync
// concurrente, mismo criterio transitorio-vs-permanente por codigo HTTP
// (ver PATRONES_ASINCRONISMO_OFFLINE.md patron 2). Ver
// OFFLINE_SUPERVISOR.md para el diseño completo.
class SupervisorSyncRepository(
    private val db: AppDatabase,
    private val api: PersonalApiService
) {
    companion object {
        private val syncMutex = Mutex()
    }

    suspend fun sync(): Boolean {
        if (syncMutex.isLocked) {
            Log.d("SUPERVISOR_SYNC", "Sincronización ya en progreso, omitiendo llamada concurrente.")
            return false
        }
        return syncMutex.withLock { doSync() }
    }

    private suspend fun doSync(): Boolean {
        var anySyncError = false
        pushRegistrosPendientes()
        anySyncError = anySyncError || pushEvidenciaPendiente()
        refreshDesdeServidor()
        return !anySyncError
    }

    // --- 1. Push de registros de horas/comentarios pendientes ---
    private suspend fun pushRegistrosPendientes() {
        val activityDao = db.supervisorActivityDao()
        val personaDao = db.supervisorPersonaDao()

        val pendientes = activityDao.getByStatus("PENDING_SYNC")

        for (actividad in pendientes) {
            try {
                val personas = personaDao.getByActivity(actividad.id)
                val request = PersonalSaveActivityRequest(
                    comments = actividad.comments,
                    all_worked_scheduled_hours = actividad.allWorkedScheduledHours ?: true,
                    personas = if (actividad.allWorkedScheduledHours == false) {
                        personas.map { PersonalPersonaHoursRequest(it.employeeId, it.workedHours ?: 0.0, it.comentario) }
                    } else {
                        emptyList()
                    }
                )

                val response = api.saveActividad(actividad.id, request)

                if (response.isSuccessful) {
                    activityDao.updateStatus(actividad.id, "SYNCED")
                } else if (response.code() == 403 || response.code() == 422) {
                    // Fallo permanente: actividad cerrada entretanto (403) o
                    // datos rechazados por validacion (422) — reintentar no
                    // cambiaria el resultado.
                    val mensaje = if (response.code() == 403) {
                        "La actividad fue cerrada antes de poder enviar el registro."
                    } else {
                        "El servidor rechazó el registro (datos inválidos)."
                    }
                    activityDao.updateStatus(actividad.id, "ERROR", mensaje)
                    Log.e("SUPERVISOR_SYNC", "Registro de actividad ${actividad.id} rechazado permanentemente: ${response.code()}")
                } else {
                    Log.e("SUPERVISOR_SYNC", "saveActividad falló para ${actividad.id}, code=${response.code()}")
                    // fallo transitorio: se deja PENDING_SYNC, se reintenta despues
                }
            } catch (e: Exception) {
                Log.e("SUPERVISOR_SYNC", "Error sincronizando registro de actividad ${actividad.id}", e)
                // excepcion (sin red, etc.) -> se deja PENDING_SYNC
            }
        }
    }

    // --- 2. Push de evidencia pendiente --- devuelve true si hubo algun error
    private suspend fun pushEvidenciaPendiente(): Boolean {
        val evidenceDao = db.supervisorEvidenceDao()
        val pendientes = evidenceDao.getByStatus("PENDING_SYNC")
        var huboError = false

        for (evidencia in pendientes) {
            try {
                val archivo = File(evidencia.localPath)
                if (!archivo.exists()) {
                    // El archivo local se perdio (desinstalo/limpio storage) —
                    // no hay nada que subir, no tiene sentido reintentar.
                    evidenceDao.updateStatus(evidencia.id, "ERROR", "El archivo local ya no existe.")
                    huboError = true
                    continue
                }

                val mimeType = if (evidencia.fileType == "video") "video/mp4" else "image/jpeg"
                val requestBody = archivo.asRequestBody(mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("files[]", evidencia.originalName, requestBody)

                val response = api.uploadEvidencias(evidencia.activityId, listOf(part))

                if (response.isSuccessful) {
                    val serverId = response.body()?.evidencias?.firstOrNull()?.id
                    if (serverId != null) {
                        evidenceDao.updateSyncData(evidencia.id, "SYNCED", serverId)
                        archivo.delete() // ya vive en R2, no hace falta la copia local
                    } else {
                        huboError = true
                    }
                } else if (response.code() == 422 || response.code() == 413 || response.code() == 403 || response.code() == 404) {
                    val mensaje = when (response.code()) {
                        413 -> "El archivo supera el límite de tamaño permitido."
                        403 -> "La actividad fue cerrada antes de poder subir la evidencia."
                        404 -> "La actividad ya no existe."
                        else -> "El servidor rechazó el archivo (formato inválido)."
                    }
                    evidenceDao.updateStatus(evidencia.id, "ERROR", mensaje)
                    huboError = true
                } else {
                    Log.e("SUPERVISOR_SYNC", "uploadEvidencias falló para evidencia ${evidencia.id}, code=${response.code()}")
                    huboError = true
                }
            } catch (e: Exception) {
                Log.e("SUPERVISOR_SYNC", "Error subiendo evidencia ${evidencia.id}", e)
                huboError = true
            }
        }

        return huboError
    }

    // --- 3. Refresh/merge desde el servidor ---
    // No pisa comments/allWorkedScheduledHours/reportedHours/personas de
    // una actividad con registrationSyncStatus = PENDING_SYNC (hay un
    // cambio local sin enviar todavia) — solo actualiza los campos de
    // solo lectura. La evidencia del servidor se upsertea por serverId
    // sin tocar las filas PENDING_SYNC/ERROR locales.
    suspend fun refreshDesdeServidor(): Boolean {
        return try {
            val response = api.getActividades()
            if (!response.success) return false

            val activityDao = db.supervisorActivityDao()
            val personaDao = db.supervisorPersonaDao()
            val evidenceDao = db.supervisorEvidenceDao()

            for (dto in response.actividades) {
                val existente = activityDao.getById(dto.id)

                if (existente == null) {
                    activityDao.insert(
                        SupervisorActivityEntity(
                            id = dto.id,
                            date = dto.date,
                            companyName = dto.company_name,
                            team = dto.team,
                            process = dto.process,
                            description = dto.description,
                            activityType = dto.activity_type,
                            shift = dto.shift,
                            estimatedHours = dto.estimated_hours,
                            closed = dto.closed,
                            comments = dto.comments,
                            allWorkedScheduledHours = dto.all_worked_scheduled_hours,
                            reportedHours = dto.reported_hours,
                            registrado = dto.registrado,
                            registrationSyncStatus = "SYNCED"
                        )
                    )
                    personaDao.insertAll(dto.personas.map {
                        SupervisorPersonaEntity(dto.id, it.id, it.nombre, it.nickname, it.worked_hours, it.comment)
                    })
                } else {
                    activityDao.updateReadOnlyFields(
                        id = dto.id,
                        date = dto.date,
                        companyName = dto.company_name,
                        team = dto.team,
                        process = dto.process,
                        description = dto.description,
                        activityType = dto.activity_type,
                        shift = dto.shift,
                        estimatedHours = dto.estimated_hours,
                        closed = dto.closed
                    )

                    if (existente.registrationSyncStatus != "PENDING_SYNC") {
                        activityDao.updateRegistration(
                            id = dto.id,
                            comments = dto.comments,
                            allWorkedScheduledHours = dto.all_worked_scheduled_hours,
                            reportedHours = dto.reported_hours,
                            registrado = dto.registrado,
                            registrationSyncStatus = "SYNCED"
                        )
                        // Limpia antes de reinsertar: evita filas huerfanas
                        // si alguien fue quitado de la actividad del lado
                        // del servidor.
                        personaDao.deleteByActivity(dto.id)
                        personaDao.insertAll(dto.personas.map {
                            SupervisorPersonaEntity(dto.id, it.id, it.nombre, it.nickname, it.worked_hours, it.comment)
                        })
                    }
                }

                // Evidencia: upsert solo lo que el servidor ya confirma
                // (por id), sin tocar filas locales PENDING_SYNC/ERROR.
                val yaConocidas = evidenceDao.getKnownServerIds(dto.id).toSet()
                dto.evidencias.filter { it.id !in yaConocidas }.forEach { ev ->
                    evidenceDao.insert(
                        SupervisorEvidenceEntity(
                            activityId = dto.id,
                            localPath = "",
                            originalName = ev.original_name,
                            fileType = ev.file_type,
                            syncStatus = "SYNCED",
                            serverId = ev.id
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            Log.e("SUPERVISOR_SYNC", "Error en refresh desde servidor", e)
            false
        }
    }
}
