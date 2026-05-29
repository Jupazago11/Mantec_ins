package com.example.mantec_ins.presentation.viewmodel

data class PendingDiagnosticItemUi(
    val elementId: Long,
    val componentId: Long,
    val componentName: String,
    val diagnosticId: Long,
    val diagnosticName: String,
    val status: String
)