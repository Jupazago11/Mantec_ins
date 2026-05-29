package com.example.mantec_ins.presentation.viewmodel

data class InspectionBatchUiState(
    val selectedElementId: Long? = null,
    val selectedComponentId: Long? = null,
    val evaluations: List<DiagnosticEvaluationUi> = emptyList(),
    val message: String? = null
)
