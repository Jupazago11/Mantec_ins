package com.example.mantec_ins.presentation.viewmodel

data class DiagnosticEvaluationUi(
    val diagnosticId: Long,
    val diagnosticName: String,
    val selectedConditionId: Long? = null,
    val recommendation: String = ""
)
