package com.example.mantec_ins.data.repository

import android.util.Log
import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.MeasurementThicknessDraftEntity
import com.example.mantec_ins.data.local.MeasurementThicknessDraftLineEntity
import com.example.mantec_ins.data.remote.MeasurementApiService
import com.example.mantec_ins.data.remote.MeasurementElementTypeDto
import android.content.Context
import com.example.mantec_ins.util.NetworkUtils
import com.example.mantec_ins.data.remote.MeasurementThicknessDraftLineRequest
import com.example.mantec_ins.data.remote.MeasurementThicknessDraftSyncRequest
import com.example.mantec_ins.data.remote.MeasurementThicknessLineDto
import com.example.mantec_ins.data.remote.MeasurementAreaDto
import com.example.mantec_ins.data.remote.MeasurementElementDto

class MeasurementThicknessRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val api: MeasurementApiService
) {

    private val dao = database.measurementThicknessDao()

    suspend fun getEnabledElementTypes(): List<MeasurementElementTypeDto> {
        return if (NetworkUtils.hasInternet(context)) {
            try {
                val response = api.getMeasurementElementTypes()

                response.elementTypes
                    .filter { it.moduleEnabled && it.creationEnabled }
                    .sortedBy { it.name.trim().lowercase() }
            } catch (e: Exception) {
                getEnabledElementTypesFromLocal()
            }
        } else {
            getEnabledElementTypesFromLocal()
        }
    }

    suspend fun getAreasByElementType(elementTypeId: Long): List<MeasurementAreaDto> {
        return if (NetworkUtils.hasInternet(context)) {
            try {
                api.getAreasByElementType(elementTypeId).areas
            } catch (e: Exception) {
                getAreasByElementTypeFromLocal(elementTypeId)
            }
        } else {
            getAreasByElementTypeFromLocal(elementTypeId)
        }
    }

    suspend fun getElementsByAreaAndElementType(
        areaId: Long,
        elementTypeId: Long
    ): List<MeasurementElementDto> {
        return if (NetworkUtils.hasInternet(context)) {
            try {
                api.getElementsByAreaAndElementType(
                    areaId = areaId,
                    elementTypeId = elementTypeId
                ).elements
            } catch (e: Exception) {
                getElementsByAreaAndElementTypeFromLocal(
                    areaId = areaId,
                    elementTypeId = elementTypeId
                )
            }
        } else {
            getElementsByAreaAndElementTypeFromLocal(
                areaId = areaId,
                elementTypeId = elementTypeId
            )
        }
    }

    private suspend fun getEnabledElementTypesFromLocal(): List<MeasurementElementTypeDto> {
        return database.measurementElementTypeAccessDao()
            .getEnabledForCreation()
            .map { item ->
                MeasurementElementTypeDto(
                    elementTypeId = item.elementTypeId,
                    name = item.name,
                    moduleEnabled = item.moduleEnabled,
                    creationEnabled = item.creationEnabled
                )
            }
            .sortedBy { it.name.trim().lowercase() }
    }

    private suspend fun getAreasByElementTypeFromLocal(
        elementTypeId: Long
    ): List<MeasurementAreaDto> {
        val elementsForType = database.elementDao()
            .getByElementType(elementTypeId)

        val areaIds = elementsForType
            .map { it.areaId }
            .distinct()
            .toSet()

        return database.areaDao()
            .getAll()
            .filter { it.id in areaIds && it.status }
            .sortedBy { it.name.trim().lowercase() }
            .map { area ->
                MeasurementAreaDto(
                    id = area.id,
                    name = area.name,
                    code = area.code
                )
            }
    }

    private suspend fun getElementsByAreaAndElementTypeFromLocal(
        areaId: Long,
        elementTypeId: Long
    ): List<MeasurementElementDto> {
        return database.elementDao()
            .getByAreaAndType(
                areaId = areaId,
                elementTypeId = elementTypeId
            )
            .filter { it.status }
            .sortedBy { it.name.trim().lowercase() }
            .map { element ->
                MeasurementElementDto(
                    id = element.id,
                    name = element.name,
                    code = element.code,
                    warehouseCode = element.warehouseCode,
                    areaId = element.areaId,
                    elementTypeId = element.elementTypeId
                )
            }
    }

    suspend fun refreshThicknessState(
        clientId: Long,
        areaId: Long,
        elementTypeId: Long,
        elementId: Long
    ) {
        val response = api.getThicknessState(elementId)

        val remoteDraft = response.draft

        if (remoteDraft != null) {
            val draftEntity = MeasurementThicknessDraftEntity(
                elementId = elementId,
                remoteDraftId = remoteDraft.id,
                clientId = clientId,
                areaId = areaId,
                elementTypeId = elementTypeId,
                createdBy = remoteDraft.createdBy,
                updatedBy = remoteDraft.updatedBy,
                remoteCreatedAt = remoteDraft.createdAt,
                remoteUpdatedAt = remoteDraft.updatedAt,
                localUpdatedAt = System.currentTimeMillis(),
                syncStatus = "SYNCED",
                lastError = null
            )

            val lineEntities = remoteDraft.lines.map { line ->
                line.toEntity(elementId = elementId)
            }

            dao.replaceDraftWithLines(
                draft = draftEntity,
                lines = lineEntities
            )
        } else {
            val existingDraft = dao.getDraftByElement(elementId)

            if (existingDraft == null) {
                val emptyDraft = MeasurementThicknessDraftEntity(
                    elementId = elementId,
                    remoteDraftId = null,
                    clientId = clientId,
                    areaId = areaId,
                    elementTypeId = elementTypeId,
                    createdBy = null,
                    updatedBy = null,
                    remoteCreatedAt = null,
                    remoteUpdatedAt = null,
                    localUpdatedAt = System.currentTimeMillis(),
                    syncStatus = "SYNCED",
                    lastError = null
                )

                val firstLine = MeasurementThicknessDraftLineEntity(
                    elementId = elementId,
                    remoteLineId = null,
                    coverNumber = 1,
                    topLeft = null,
                    topCenter = null,
                    topRight = null,
                    bottomLeft = null,
                    bottomCenter = null,
                    bottomRight = null,
                    hardnessLeft = null,
                    hardnessCenter = null,
                    hardnessRight = null
                )

                dao.replaceDraftWithLines(
                    draft = emptyDraft,
                    lines = listOf(firstLine)
                )
            }
        }
    }

    suspend fun getLocalDraft(elementId: Long): MeasurementThicknessDraftEntity? {
        return dao.getDraftByElement(elementId)
    }

    suspend fun getLocalLines(elementId: Long): List<MeasurementThicknessDraftLineEntity> {
        return dao.getLinesByElement(elementId)
    }

    suspend fun saveLocalDraft(
        draft: MeasurementThicknessDraftEntity,
        lines: List<MeasurementThicknessDraftLineEntity>
    ) {
        val updatedDraft = draft.copy(
            localUpdatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING_SYNC",
            lastError = null
        )

        dao.replaceDraftWithLines(
            draft = updatedDraft,
            lines = lines
        )
    }

    suspend fun syncLocalDraft(elementId: Long): Boolean {
        val draft = dao.getDraftByElement(elementId) ?: return false
        val lines = dao.getLinesByElement(elementId)

        if (lines.isEmpty()) {
            dao.updateDraftSyncStatus(
                elementId = elementId,
                syncStatus = "ERROR",
                lastError = "No hay líneas para sincronizar."
            )
            return false
        }

        return try {
            val request = MeasurementThicknessDraftSyncRequest(
                lastKnownUpdatedAt = draft.remoteUpdatedAt,
                lines = lines
                    .sortedBy { it.coverNumber }
                    .map { it.toRequest() }
            )

            val response = api.syncThicknessDraft(
                elementId = elementId,
                request = request
            )

            if (response.code() == 409) {
                dao.updateDraftSyncStatus(
                    elementId = elementId,
                    syncStatus = "CONFLICT",
                    lastError = "El borrador fue modificado desde web u otro dispositivo."
                )
                return false
            }

            if (!response.isSuccessful) {
                dao.updateDraftSyncStatus(
                    elementId = elementId,
                    syncStatus = "ERROR",
                    lastError = "Error HTTP ${response.code()} al sincronizar."
                )
                return false
            }

            val body = response.body()

            if (body?.draft == null) {
                dao.updateDraftSyncStatus(
                    elementId = elementId,
                    syncStatus = "ERROR",
                    lastError = "Respuesta sin borrador sincronizado."
                )
                return false
            }

            val syncedDraft = draft.copy(
                remoteDraftId = body.draft.id,
                createdBy = body.draft.createdBy,
                updatedBy = body.draft.updatedBy,
                remoteCreatedAt = body.draft.createdAt,
                remoteUpdatedAt = body.draft.updatedAt,
                localUpdatedAt = System.currentTimeMillis(),
                syncStatus = "SYNCED",
                lastError = null
            )

            val syncedLines = body.draft.lines.map { it.toEntity(elementId) }

            dao.replaceDraftWithLines(
                draft = syncedDraft,
                lines = syncedLines
            )

            true
        } catch (e: Exception) {
            Log.e("MEASUREMENT_SYNC", "Error sincronizando borrador elementId=$elementId", e)

            dao.updateDraftSyncStatus(
                elementId = elementId,
                syncStatus = "ERROR",
                lastError = e.message ?: "Error desconocido"
            )

            false
        }
    }

    suspend fun syncAllPendingDrafts(): Int {
        val pending = dao.getPendingDrafts()
        var count = 0

        pending.forEach { draft ->
            if (syncLocalDraft(draft.elementId)) {
                count++
            }
        }

        return count
    }

    suspend fun countPendingDrafts(): Int {
        return dao.countPendingDrafts()
    }

    suspend fun hasEnabledElementTypes(): Boolean {
        return database.measurementElementTypeAccessDao().getEnabledForCreation().isNotEmpty()
    }

    suspend fun saveLocalDraftAndTrySync(
        draft: MeasurementThicknessDraftEntity,
        lines: List<MeasurementThicknessDraftLineEntity>
    ): SaveAndSyncResult {
        saveLocalDraft(
            draft = draft,
            lines = lines
        )

        if (!NetworkUtils.hasInternet(context)) {
            return SaveAndSyncResult(
                savedLocal = true,
                synced = false,
                message = "Borrador guardado localmente. Se sincronizará cuando tengas conexión."
            )
        }

        val synced = syncLocalDraft(draft.elementId)

        return if (synced) {
            SaveAndSyncResult(
                savedLocal = true,
                synced = true,
                message = "Borrador guardado y sincronizado correctamente."
            )
        } else {
            val updatedDraft = getLocalDraft(draft.elementId)

            SaveAndSyncResult(
                savedLocal = true,
                synced = false,
                message = updatedDraft?.lastError
                    ?: "Borrador guardado localmente, pero no se pudo sincronizar ahora."
            )
        }
    }

    data class SaveAndSyncResult(
        val savedLocal: Boolean,
        val synced: Boolean,
        val message: String
    )

    private fun MeasurementThicknessLineDto.toEntity(
        elementId: Long
    ): MeasurementThicknessDraftLineEntity {
        return MeasurementThicknessDraftLineEntity(
            elementId = elementId,
            remoteLineId = id,
            coverNumber = coverNumber,
            topLeft = topLeft,
            topCenter = topCenter,
            topRight = topRight,
            bottomLeft = bottomLeft,
            bottomCenter = bottomCenter,
            bottomRight = bottomRight,
            hardnessLeft = hardnessLeft,
            hardnessCenter = hardnessCenter,
            hardnessRight = hardnessRight
        )
    }

    private fun MeasurementThicknessDraftLineEntity.toRequest(): MeasurementThicknessDraftLineRequest {
        return MeasurementThicknessDraftLineRequest(
            coverNumber = coverNumber,
            topLeft = topLeft,
            topCenter = topCenter,
            topRight = topRight,
            bottomLeft = bottomLeft,
            bottomCenter = bottomCenter,
            bottomRight = bottomRight,
            hardnessLeft = hardnessLeft,
            hardnessCenter = hardnessCenter,
            hardnessRight = hardnessRight
        )
    }
}