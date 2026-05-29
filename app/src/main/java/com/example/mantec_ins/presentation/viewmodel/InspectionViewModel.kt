package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.local.EvidenceEntity
import com.example.mantec_ins.data.local.ReportDetailEntity
import com.example.mantec_ins.data.local.ReportEntity
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class InspectionViewModel(
    private val repository: InspectionLocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState

    fun setSelectedElement(elementId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedElementId = elementId,
            selectedComponentId = null,
            selectedDiagnosticId = null,
            selectedConditionId = null,
            recommendation = "",
            evidences = emptyList(),
            isBeltChange = false,
            message = null
        )
    }

    fun setSelectedComponent(componentId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedComponentId = componentId,
            selectedDiagnosticId = null,
            selectedConditionId = null,
            recommendation = "",
            evidences = emptyList(),
            isBeltChange = false,
            message = null
        )
    }


    fun setSelectedDiagnostic(diagnosticId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedDiagnosticId = diagnosticId,
            selectedConditionId = null,
            recommendation = "",
            evidences = emptyList(),
            isBeltChange = false,
            message = null
        )
    }


    fun setSelectedCondition(conditionId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedConditionId = conditionId,
            message = null
        )
    }

    fun setRecommendation(value: String) {
        _uiState.value = _uiState.value.copy(
            recommendation = value,
            message = null
        )
    }

    fun addEvidence(path: String, type: String) {
        val current = _uiState.value.evidences
        _uiState.value = _uiState.value.copy(
            evidences = current + MediaEvidenceUi(path = path, type = type),
            message = null
        )
    }

    fun removeEvidence(path: String) {
        _uiState.value = _uiState.value.copy(
            evidences = _uiState.value.evidences.filterNot { it.path == path },
            message = null
        )
    }

    fun clearSelectionsFromAreaChange() {
        _uiState.value = _uiState.value.copy(
            selectedElementId = null,
            selectedComponentId = null,
            selectedDiagnosticId = null,
            selectedConditionId = null,
            recommendation = "",
            evidences = emptyList(),
            isBeltChange = false,
            message = null
        )
    }

    fun setIsBeltChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(isBeltChange = value)
    }



    fun saveInspectionReport(
        clientId: Long,
        areaId: Long,
        userId: Long
    ) {
        val state = _uiState.value
        val elementId = state.selectedElementId
        val componentId = state.selectedComponentId
        val diagnosticId = state.selectedDiagnosticId
        val conditionId = state.selectedConditionId

        if (areaId <= 0L) {
            _uiState.value = state.copy(
                message = "Debes seleccionar un área",
                saveSuccess = false
            )
            return
        }

        if (elementId == null) {
            _uiState.value = state.copy(
                message = "Debes seleccionar un activo",
                saveSuccess = false
            )
            return
        }

        if (componentId == null) {
            _uiState.value = state.copy(
                message = "Debes seleccionar un componente",
                saveSuccess = false
            )
            return
        }

        if (diagnosticId == null) {
            _uiState.value = state.copy(
                message = "Debes seleccionar un diagnóstico",
                saveSuccess = false
            )
            return
        }

        if (conditionId == null) {
            _uiState.value = state.copy(
                message = "Debes seleccionar una condición",
                saveSuccess = false
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(
                    isSaving = true,
                    saveSuccess = false,
                    message = null
                )

                val reportLocalId = UUID.randomUUID().toString()

                val calendar = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    minimalDaysInFirstWeek = 4
                }

                val week = calendar.get(Calendar.WEEK_OF_YEAR)
                val year = calendar.weekYear

                val executionDate = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(calendar.time)

                val report = ReportEntity(
                    localId = reportLocalId,
                    clientId = clientId,
                    areaId = areaId,
                    elementId = elementId,
                    userId = userId,
                    createdAt = System.currentTimeMillis().toString(),
                    status = "PENDING_SYNC"
                )

                repository.saveReport(report)

                android.util.Log.d(
                    "RECOMMENDATION_DEBUG",
                    "Antes de guardar local: '${state.recommendation}'"
                )

                repository.saveReportDetail(
                    ReportDetailEntity(
                        reportLocalId = reportLocalId,
                        componentId = componentId,
                        diagnosticId = diagnosticId,
                        conditionId = conditionId,
                        recommendation = state.recommendation.ifBlank { null },
                        week = week,
                        year = year,
                        executionDate = executionDate,
                        syncStatus = "PENDING_SYNC",
                        isBeltChange = state.isBeltChange
                    )
                )

                state.evidences.forEach { media ->
                    repository.saveEvidence(
                        EvidenceEntity(
                            reportLocalId = reportLocalId,
                            reportDetailId = null,
                            localPath = media.path,
                            type = media.type,
                            syncStatus = "PENDING_SYNC"
                        )
                    )
                }

                _uiState.value = InspectionUiState(
                    selectedElementId = state.selectedElementId,
                    selectedComponentId = state.selectedComponentId,
                    selectedDiagnosticId = null,
                    selectedConditionId = null,
                    recommendation = "",
                    isBeltChange = false,
                    evidences = emptyList(),
                    isSaving = false,
                    saveSuccess = true,
                    message = "Reporte guardado localmente"
                )

            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isSaving = false,
                    saveSuccess = false,
                    message = "Error al guardar el reporte"
                )
            }
        }
    }


    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(
            saveSuccess = false,
            message = null
        )
    }





}