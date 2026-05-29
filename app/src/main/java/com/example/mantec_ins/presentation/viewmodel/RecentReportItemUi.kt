package com.example.mantec_ins.presentation.viewmodel

data class RecentReportItemUi(
    val reportLocalId: String,
    val elementName: String,
    val componentName: String,
    val diagnosticName: String,
    val conditionName: String,
    val executionDate: String,
    val reportStatus: String,
    val detailSyncStatus: String
)