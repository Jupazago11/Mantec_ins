package com.example.mantec_ins.presentation.viewmodel

data class ReportDetailItemUi(
    val componentId: Long,
    val componentName: String,
    val diagnosticId: Long,
    val diagnosticName: String,
    val conditionId: Long,
    val conditionName: String,
    val recommendation: String?,
    val week: Int,
    val year: Int,
    val executionDate: String,
    val syncStatus: String
)