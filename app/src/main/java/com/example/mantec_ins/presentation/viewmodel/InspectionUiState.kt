package com.example.mantec_ins.presentation.viewmodel

data class InspectionUiState(
    val selectedElementId: Long? = null,
    val selectedComponentId: Long? = null,
    val selectedDiagnosticId: Long? = null,
    val selectedConditionId: Long? = null,
    val recommendation: String = "",
    val evidences: List<MediaEvidenceUi> = emptyList(),
    val message: String? = null,
    val saveSuccess: Boolean = false,
    val isSaving: Boolean = false,
    val isBeltChange: Boolean = false
)
