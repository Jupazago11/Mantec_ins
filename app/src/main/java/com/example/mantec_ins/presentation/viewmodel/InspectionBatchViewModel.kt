package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.local.ReportDetailEntity
import com.example.mantec_ins.data.local.ReportEntity
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

class InspectionBatchViewModel(
    private val catalogRepository: CatalogLocalRepository,
    private val inspectionRepository: InspectionLocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionBatchUiState())
    val uiState: StateFlow<InspectionBatchUiState> = _uiState

    fun setSelectedElement(elementId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedElementId = elementId,
            selectedComponentId = null,
            evaluations = emptyList(),
            message = null
        )
    }

    fun loadDiagnosticsForComponent(componentId: Long) {
        viewModelScope.launch {
            val diagnostics = catalogRepository.getDiagnosticsByComponent(componentId)

            _uiState.value = _uiState.value.copy(
                selectedComponentId = componentId,
                evaluations = diagnostics.map {
                    DiagnosticEvaluationUi(
                        diagnosticId = it.id,
                        diagnosticName = it.name
                    )
                },
                message = null
            )
        }
    }

    fun updateCondition(diagnosticId: Long, conditionId: Long) {
        _uiState.value = _uiState.value.copy(
            evaluations = _uiState.value.evaluations.map { item ->
                if (item.diagnosticId == diagnosticId) {
                    item.copy(selectedConditionId = conditionId)
                } else item
            },
            message = null
        )
    }

    fun updateRecommendation(diagnosticId: Long, recommendation: String) {
        _uiState.value = _uiState.value.copy(
            evaluations = _uiState.value.evaluations.map { item ->
                if (item.diagnosticId == diagnosticId) {
                    item.copy(recommendation = recommendation)
                } else item
            },
            message = null
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun saveBatchInspection(
        clientId: Long,
        areaId: Long,
        userId: Long
    ) {
        val state = _uiState.value
        val elementId = state.selectedElementId
        val componentId = state.selectedComponentId

        if (elementId == null) {
            _uiState.value = state.copy(message = "Debes seleccionar un activo")
            return
        }

        if (componentId == null) {
            _uiState.value = state.copy(message = "Debes seleccionar un componente")
            return
        }

        val validEvaluations = state.evaluations.filter { it.selectedConditionId != null }

        if (validEvaluations.isEmpty()) {
            _uiState.value = state.copy(message = "Debes evaluar al menos un diagnóstico")
            return
        }

        viewModelScope.launch {
            val reportLocalId = UUID.randomUUID().toString()
            val today = LocalDate.now()
            val weekFields = WeekFields.of(Locale.getDefault())
            val week = today.get(weekFields.weekOfWeekBasedYear())
            val year = today.get(weekFields.weekBasedYear())
            val executionDate = today.toString()

            val report = ReportEntity(
                localId = reportLocalId,
                clientId = clientId,
                areaId = areaId,
                elementId = elementId,
                userId = userId,
                createdAt = System.currentTimeMillis().toString(),
                status = "PENDING_SYNC"
            )

            inspectionRepository.saveReport(report)

            validEvaluations.forEach { evaluation ->
                inspectionRepository.saveReportDetail(
                    ReportDetailEntity(
                        reportLocalId = reportLocalId,
                        componentId = componentId,
                        diagnosticId = evaluation.diagnosticId,
                        conditionId = evaluation.selectedConditionId!!,
                        recommendation = evaluation.recommendation.ifBlank { null },
                        week = week,
                        year = year,
                        executionDate = executionDate,
                        syncStatus = "PENDING_SYNC"
                    )
                )
            }

            _uiState.value = InspectionBatchUiState(
                message = "Inspección guardada localmente"
            )
        }
    }
}
