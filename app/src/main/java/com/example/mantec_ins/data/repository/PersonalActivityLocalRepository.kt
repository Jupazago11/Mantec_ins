package com.example.mantec_ins.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.SupervisorActivityEntity
import com.example.mantec_ins.data.local.SupervisorEvidenceEntity
import com.example.mantec_ins.data.local.SupervisorPersonaEntity
import java.io.File
import java.util.UUID

// Calco de InspectionLocalRepository (Inspector) — 100% Room, sin
// llamadas a Retrofit. Es la fuente de verdad que lee la UI: nunca
// espera red para pintar (ver PATRONES_ASINCRONISMO_OFFLINE.md patron
// 1). La sincronizacion real vive aparte, en SupervisorSyncRepository.
class PersonalActivityLocalRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    suspend fun getActividades(): List<SupervisorActivityEntity> {
        return database.supervisorActivityDao().getAll()
    }

    suspend fun getPersonas(activityId: Long): List<SupervisorPersonaEntity> {
        return database.supervisorPersonaDao().getByActivity(activityId)
    }

    suspend fun getEvidencias(activityId: Long): List<SupervisorEvidenceEntity> {
        return database.supervisorEvidenceDao().getByActivity(activityId)
    }

    // Escritura local inmediata (PENDING_SYNC) — el guardado del usuario
    // NUNCA espera a la red, igual que InspectionViewModel.saveReport().
    suspend fun guardarRegistroLocal(
        activityId: Long,
        comments: String?,
        allWorkedScheduledHours: Boolean,
        personasHoras: Map<Long, Double>
    ) {
        val dao = database.supervisorActivityDao()
        val personaDao = database.supervisorPersonaDao()

        val reportedHours = if (allWorkedScheduledHours) {
            dao.getById(activityId)?.estimatedHours
        } else {
            personasHoras.values.sumOf { it }
        }

        dao.updateRegistration(
            id = activityId,
            comments = comments,
            allWorkedScheduledHours = allWorkedScheduledHours,
            reportedHours = reportedHours,
            registrado = true,
            registrationSyncStatus = "PENDING_SYNC"
        )

        if (!allWorkedScheduledHours) {
            personasHoras.forEach { (employeeId, horas) ->
                personaDao.updateWorkedHours(activityId, employeeId, horas)
            }
        }
    }

    // Independiente de guardarRegistroLocal(): el modal de comentario por
    // trabajador (pedido 2026-09-22) guarda/borra sin tocar horas ni el
    // comentario general de la actividad. comentario = null o "" borra.
    suspend fun guardarComentarioPersonaLocal(activityId: Long, employeeId: Long, comentario: String?) {
        val texto = comentario?.trim().takeUnless { it.isNullOrEmpty() }
        database.supervisorPersonaDao().updateComentario(activityId, employeeId, texto)
        database.supervisorActivityDao().updateStatus(activityId, "PENDING_SYNC")
    }

    // Copia el archivo desde la URI del picker/camara a almacenamiento
    // PRIVADO de la app (filesDir) ANTES de encolarlo — mejora consciente
    // sobre el patron del Inspector (que guarda la URI publica de
    // MediaStore tal cual, con riesgo de perderla si el sistema limpia
    // esa carpeta antes del sync). Retorna null si no se pudo leer la URI.
    suspend fun encolarEvidenciaDesdeUri(activityId: Long, uri: Uri): Long? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val fileType = if (mimeType.startsWith("video/")) "video" else "image"
        val originalName = queryFileName(uri) ?: "evidencia_${System.currentTimeMillis()}"
        val extension = originalName.substringAfterLast('.', "bin")

        val destino = File(context.filesDir, "supervisor_evidence").apply { mkdirs() }
        val destinoFile = File(destino, "${UUID.randomUUID()}.$extension")

        return try {
            resolver.openInputStream(uri)?.use { input ->
                destinoFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            database.supervisorEvidenceDao().insert(
                SupervisorEvidenceEntity(
                    activityId = activityId,
                    localPath = destinoFile.absolutePath,
                    originalName = originalName,
                    fileType = fileType,
                    syncStatus = "PENDING_SYNC"
                )
            )
        } catch (e: Exception) {
            Log.e("PERSONAL_ACTIVITY_LOCAL_REPO", "Error copiando evidencia desde uri=$uri", e)
            destinoFile.delete()
            null
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun borrarEvidenciaLocal(evidenceId: Long) {
        val dao = database.supervisorEvidenceDao()
        val evidencia = dao.getById(evidenceId)
        dao.deleteById(evidenceId)
        // Si ya se habia subido (serverId != null) el borrado remoto lo
        // maneja el ViewModel via PersonalActivityRepository antes de
        // llegar aca. Si nunca se subio, solo queda limpiar el archivo
        // local propio para no dejar basura en filesDir.
        evidencia?.let { File(it.localPath).delete() }
    }

    suspend fun getPendientesDeRegistro(): List<SupervisorActivityEntity> {
        return database.supervisorActivityDao().getByStatus("PENDING_SYNC")
    }

    suspend fun getEvidenciasPendientes(): List<SupervisorEvidenceEntity> {
        return database.supervisorEvidenceDao().getByStatus("PENDING_SYNC")
    }

    suspend fun clearAll() {
        database.supervisorEvidenceDao().deleteAll()
        database.supervisorPersonaDao().deleteAll()
        database.supervisorActivityDao().deleteAll()
    }
}
