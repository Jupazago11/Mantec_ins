package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportDetailViewModel(
    private val inspectionRepository: InspectionLocalRepository,
    private val catalogRepository: CatalogLocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = _uiState

    fun loadReportDetail(reportLocalId: String) {
        viewModelScope.launch {
            val rawDetails = inspectionRepository.getReportDetails(reportLocalId)
            val evidences = inspectionRepository.getReportEvidences(reportLocalId)

            val mappedDetails = rawDetails.map { detail ->
                val component = catalogRepository.getComponentById(detail.componentId)
                val diagnostic = catalogRepository.getDiagnosticById(detail.diagnosticId)
                val condition = catalogRepository.getConditionById(detail.conditionId)

                ReportDetailItemUi(
                    componentId = detail.componentId,
                    componentName = component?.name ?: "Componente ${detail.componentId}",
                    diagnosticId = detail.diagnosticId,
                    diagnosticName = diagnostic?.name ?: "Diagnóstico ${detail.diagnosticId}",
                    conditionId = detail.conditionId,
                    conditionName = condition?.name ?: "Condición ${detail.conditionId}",
                    recommendation = detail.recommendation,
                    week = detail.week,
                    year = detail.year,
                    executionDate = detail.executionDate,
                    syncStatus = detail.syncStatus
                )
            }

            _uiState.value = ReportDetailUiState(
                selectedReportId = reportLocalId,
                details = mappedDetails,
                evidences = evidences
            )
        }
    }

    fun clearSelection() {
        _uiState.value = ReportDetailUiState()
    }
}