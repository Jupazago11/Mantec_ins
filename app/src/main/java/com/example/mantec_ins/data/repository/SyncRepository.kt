package com.example.mantec_ins.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.remote.SyncApiService
import com.example.mantec_ins.sync.SyncReportRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class SyncRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val api: SyncApiService
) {

    companion object {
        private val syncMutex = Mutex()
    }

    suspend fun syncPendingReports(): Int {
        if (syncMutex.isLocked) {
            Log.d("SYNC_REPOSITORY", "Sincronización ya en progreso, omitiendo llamada concurrente.")
            return 0
        }

        return syncMutex.withLock {
            doSyncPendingReports()
        }
    }

    private suspend fun doSyncPendingReports(): Int {
        val reportDao = db.reportDao()
        val reportDetailDao = db.reportDetailDao()
        val evidenceDao = db.evidenceDao()

        val pendingReports = reportDao.getByStatus("PENDING_SYNC")
        var syncedReportsCount = 0

        for (report in pendingReports) {
            try {
                val detail = reportDetailDao.getSingleByReport(report.localId)
                if (detail == null) {
                    Log.w("SYNC_REPOSITORY", "Reporte ${report.localId} sin detail asociado")
                    continue
                }

                Log.d(
                    "SYNC_REPOSITORY",
                    "Iniciando sync de reporte localId=${report.localId}"
                )

                val serverReportDetailId: Long

                if (detail.syncStatus == "SYNCED" && detail.serverId != null) {
                    // El detalle ya fue aceptado por el servidor en un intento anterior.
                    // No reenviar: solo reintentar las evidencias pendientes con el ID ya guardado.
                    Log.d(
                        "SYNC_REPOSITORY",
                        "Reporte ${report.localId} ya enviado al servidor (serverId=${detail.serverId}). Reintentando solo evidencias."
                    )
                    serverReportDetailId = detail.serverId
                } else {
                    val reportRequest = SyncReportRequest(
                        local_report_id = report.localId,
                        client_id = report.clientId,
                        area_id = report.areaId,
                        element_id = report.elementId,
                        component_id = detail.componentId,
                        diagnostic_id = detail.diagnosticId,
                        condition_id = detail.conditionId,
                        recommendation = detail.recommendation,
                        week = detail.week,
                        year = detail.year,
                        execution_date = detail.executionDate,
                        is_belt_change = detail.isBeltChange
                    )

                    val reportResponse = api.syncReport(reportRequest)

                    if (!reportResponse.isSuccessful) {
                        Log.e(
                            "SYNC_REPOSITORY",
                            "syncReport falló para ${report.localId}. code=${reportResponse.code()}"
                        )
                        continue
                    }

                    val reportBody = reportResponse.body()
                    if (reportBody == null) {
                        Log.e(
                            "SYNC_REPOSITORY",
                            "syncReport respondió sin body para ${report.localId}"
                        )
                        continue
                    }

                    serverReportDetailId = reportBody.server_report_detail_id

                    reportDetailDao.updateSyncData(
                        reportLocalId = report.localId,
                        serverId = serverReportDetailId,
                        syncStatus = "SYNCED"
                    )
                }

                val evidences = evidenceDao.getByReport(report.localId)
                var allFilesSynced = true

                evidences.forEachIndexed { index, evidence ->
                    if (evidence.syncStatus == "SYNCED") {
                        return@forEachIndexed
                    }

                    val multipart = buildMultipartFromUri(
                        uriString = evidence.localPath,
                        fieldName = "file"
                    )

                    if (multipart == null) {
                        Log.e(
                            "SYNC_REPOSITORY",
                            "No se pudo construir multipart para evidencia id=${evidence.id}"
                        )
                        allFilesSynced = false
                        return@forEachIndexed
                    }

                    val sortOrderBody = index.toString()
                        .toRequestBody("text/plain".toMediaType())

                    val uploadResponse = api.uploadReportFile(
                        reportDetailId = serverReportDetailId,
                        file = multipart,
                        sortOrder = sortOrderBody
                    )

                    if (uploadResponse.isSuccessful) {
                        val uploadBody = uploadResponse.body()

                        if (uploadBody != null) {
                            evidenceDao.updateSyncData(
                                id = evidence.id,
                                syncStatus = "SYNCED",
                                serverFileId = uploadBody.file_id
                            )
                        } else {
                            Log.e(
                                "SYNC_REPOSITORY",
                                "uploadReportFile sin body para evidencia id=${evidence.id}"
                            )
                            allFilesSynced = false
                        }
                    } else {
                        Log.e(
                            "SYNC_REPOSITORY",
                            "uploadReportFile falló para evidencia id=${evidence.id}, code=${uploadResponse.code()}"
                        )
                        allFilesSynced = false
                    }
                }

                if (allFilesSynced) {
                    reportDao.updateStatus(report.localId, "SYNCED")
                    syncedReportsCount++

                    Log.d(
                        "SYNC_REPOSITORY",
                        "Reporte ${report.localId} sincronizado completamente"
                    )
                } else {
                    Log.w(
                        "SYNC_REPOSITORY",
                        "Reporte ${report.localId} sincronizó detalle pero no todas las evidencias"
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "SYNC_REPOSITORY",
                    "Error sincronizando reporte ${report.localId}",
                    e
                )
            }
        }

        Log.d(
            "SYNC_REPOSITORY",
            "Sincronización finalizada. syncedReportsCount=$syncedReportsCount"
        )

        return syncedReportsCount
    }

    private fun buildMultipartFromUri(
        uriString: String,
        fieldName: String
    ): MultipartBody.Part? {
        return try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            val fileName = queryFileName(uri) ?: "upload_${System.currentTimeMillis()}"

            val inputStream = resolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }

            val requestBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()
                override fun contentLength(): Long = bytes.size.toLong()

                override fun writeTo(sink: okio.BufferedSink) {
                    sink.write(bytes)
                }
            }

            MultipartBody.Part.createFormData(
                fieldName,
                fileName,
                requestBody
            )
        } catch (e: Exception) {
            Log.e("SYNC_REPOSITORY", "Error creando multipart desde uri=$uriString", e)
            null
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_REPOSITORY", "Error obteniendo nombre de archivo para uri=$uri", e)
            null
        }
    }
}
