package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.local.MeasurementThicknessDraftEntity
import com.example.mantec_ins.data.local.MeasurementThicknessDraftLineEntity
import com.example.mantec_ins.data.remote.MeasurementAreaDto
import com.example.mantec_ins.data.remote.MeasurementElementDto
import com.example.mantec_ins.data.remote.MeasurementElementTypeDto
import com.example.mantec_ins.data.repository.MeasurementThicknessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeasurementThicknessViewModel(
    private val repository: MeasurementThicknessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeasurementThicknessUiState())
    val uiState: StateFlow<MeasurementThicknessUiState> = _uiState

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                message = null
            )

            try {
                val elementTypes = repository.getEnabledElementTypes()

                val singleType = elementTypes.singleOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    elementTypes = elementTypes,
                    shouldShowElementTypeSelector = elementTypes.size >= 2,
                    selectedElementTypeId = singleType?.elementTypeId,
                    selectedElementTypeName = singleType?.name ?: "",
                    errorMessage = if (elementTypes.isEmpty()) {
                        "No tienes tipos de activo habilitados para mediciones."
                    } else {
                        null
                    }
                )

                if (singleType != null) {
                    selectElementType(singleType)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error cargando tipos de activo para mediciones."
                )
            }
        }
    }

    fun selectElementType(elementType: MeasurementElementTypeDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedElementTypeId = elementType.elementTypeId,
                selectedElementTypeName = elementType.name,
                selectedAreaId = null,
                selectedAreaName = "",
                selectedElementId = null,
                selectedElementName = "",
                areas = emptyList(),
                elements = emptyList(),
                draft = null,
                lines = emptyList(),
                isLoading = true,
                errorMessage = null,
                message = null
            )

            try {
                val areas = repository.getAreasByElementType(elementType.elementTypeId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    areas = areas,
                    errorMessage = if (areas.isEmpty()) {
                        "No hay áreas con activos disponibles para este tipo de activo."
                    } else {
                        null
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error cargando áreas para mediciones."
                )
            }
        }
    }

    fun selectArea(area: MeasurementAreaDto) {
        val elementTypeId = _uiState.value.selectedElementTypeId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedAreaId = area.id,
                selectedAreaName = area.name,
                selectedElementId = null,
                selectedElementName = "",
                elements = emptyList(),
                draft = null,
                lines = emptyList(),
                isLoading = true,
                errorMessage = null,
                message = null
            )

            try {
                val elements = repository.getElementsByAreaAndElementType(
                    areaId = area.id,
                    elementTypeId = elementTypeId
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    elements = elements,
                    errorMessage = if (elements.isEmpty()) {
                        "No hay activos disponibles en esta área."
                    } else {
                        null
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error cargando activos para mediciones."
                )
            }
        }
    }

    fun selectElement(
        clientId: Long,
        element: MeasurementElementDto
    ) {
        val areaId = _uiState.value.selectedAreaId ?: element.areaId
        val elementTypeId = _uiState.value.selectedElementTypeId ?: element.elementTypeId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedElementId = element.id,
                selectedElementName = element.name,
                isLoading = true,
                errorMessage = null,
                message = null
            )

            try {
                repository.refreshThicknessState(
                    clientId = clientId,
                    areaId = areaId,
                    elementTypeId = elementTypeId,
                    elementId = element.id
                )

                val draft = repository.getLocalDraft(element.id)
                val lines = repository.getLocalLines(element.id)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    draft = draft,
                    lines = lines.ifEmpty {
                        listOf(newEmptyLine(elementId = element.id, coverNumber = 1))
                    }
                )
            } catch (e: Exception) {
                val localDraft = repository.getLocalDraft(element.id)
                val localLines = repository.getLocalLines(element.id)

                val fallbackLines = localLines.ifEmpty {
                    listOf(newEmptyLine(elementId = element.id, coverNumber = 1))
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    draft = localDraft,
                    lines = fallbackLines,
                    errorMessage = if (localDraft == null) {
                        "Sin conexión. Puedes crear un borrador local para este activo."
                    } else {
                        "Sin conexión. Se cargó el borrador local."
                    }
                )
            }
        }
    }

    fun updateLineValue(
        coverNumber: Int,
        field: String,
        rawValue: String
    ) {
        val parsed = rawValue.replace(",", ".").toDoubleOrNull()

        val updatedLines = _uiState.value.lines.map { line ->
            if (line.coverNumber != coverNumber) {
                line
            } else {
                when (field) {
                    "topLeft" -> line.copy(topLeft = parsed)
                    "topCenter" -> line.copy(topCenter = parsed)
                    "topRight" -> line.copy(topRight = parsed)

                    "bottomLeft" -> line.copy(bottomLeft = parsed)
                    "bottomCenter" -> line.copy(bottomCenter = parsed)
                    "bottomRight" -> line.copy(bottomRight = parsed)

                    "hardnessLeft" -> line.copy(hardnessLeft = parsed)
                    "hardnessCenter" -> line.copy(hardnessCenter = parsed)
                    "hardnessRight" -> line.copy(hardnessRight = parsed)

                    else -> line
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            lines = updatedLines,
            message = null,
            errorMessage = null
        )
    }

    fun addCover() {
        val elementId = _uiState.value.selectedElementId ?: return

        val nextCoverNumber = ((_uiState.value.lines.maxOfOrNull { it.coverNumber }) ?: 0) + 1

        _uiState.value = _uiState.value.copy(
            lines = _uiState.value.lines + newEmptyLine(
                elementId = elementId,
                coverNumber = nextCoverNumber
            ),
            message = null,
            errorMessage = null
        )
    }

    fun removeLastCover() {
        val currentLines = _uiState.value.lines

        if (currentLines.size <= 1) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Debe existir al menos una cubierta."
            )
            return
        }

        val maxCover = currentLines.maxOf { it.coverNumber }

        _uiState.value = _uiState.value.copy(
            lines = currentLines.filterNot { it.coverNumber == maxCover },
            message = null,
            errorMessage = null
        )
    }

    fun saveDraft(
        clientId: Long,
        userId: Long
    ) {
        val state = _uiState.value
        val elementId = state.selectedElementId
        val areaId = state.selectedAreaId
        val elementTypeId = state.selectedElementTypeId

        if (elementId == null || areaId == null || elementTypeId == null) {
            _uiState.value = state.copy(
                errorMessage = "Selecciona tipo de activo, área y activo antes de guardar."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isSavingLocal = true,
                isSyncing = false,
                errorMessage = null,
                message = null
            )

            try {
                val existingDraft = state.draft

                val draft = MeasurementThicknessDraftEntity(
                    elementId = elementId,
                    remoteDraftId = existingDraft?.remoteDraftId,
                    clientId = clientId,
                    areaId = areaId,
                    elementTypeId = elementTypeId,
                    createdBy = existingDraft?.createdBy ?: userId,
                    updatedBy = userId,
                    remoteCreatedAt = existingDraft?.remoteCreatedAt,
                    remoteUpdatedAt = existingDraft?.remoteUpdatedAt,
                    localUpdatedAt = System.currentTimeMillis(),
                    syncStatus = "PENDING_SYNC",
                    lastError = null
                )

                val normalizedLines = state.lines
                    .sortedBy { it.coverNumber }
                    .mapIndexed { index, line ->
                        line.copy(
                            id = 0,
                            elementId = elementId,
                            coverNumber = index + 1
                        )
                    }

                val result = repository.saveLocalDraftAndTrySync(
                    draft = draft,
                    lines = normalizedLines
                )

                val savedDraft = repository.getLocalDraft(elementId)
                val savedLines = repository.getLocalLines(elementId)

                _uiState.value = _uiState.value.copy(
                    isSavingLocal = false,
                    isSyncing = false,
                    draft = savedDraft,
                    lines = savedLines,
                    message = if (result.synced) result.message else null,
                    errorMessage = if (!result.synced) result.message else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingLocal = false,
                    isSyncing = false,
                    errorMessage = "Error guardando borrador local."
                )
            }
        }
    }

    fun syncCurrentDraft() {
        val elementId = _uiState.value.selectedElementId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                message = null,
                errorMessage = null
            )

            val synced = repository.syncLocalDraft(elementId)

            val draft = repository.getLocalDraft(elementId)
            val lines = repository.getLocalLines(elementId)

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                draft = draft,
                lines = lines,
                message = if (synced) {
                    "Borrador sincronizado correctamente."
                } else {
                    null
                },
                errorMessage = if (!synced) {
                    draft?.lastError ?: "No se pudo sincronizar el borrador."
                } else {
                    null
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            message = null,
            errorMessage = null
        )
    }

    private fun newEmptyLine(
        elementId: Long,
        coverNumber: Int
    ): MeasurementThicknessDraftLineEntity {
        return MeasurementThicknessDraftLineEntity(
            elementId = elementId,
            remoteLineId = null,
            coverNumber = coverNumber,
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
    }
}