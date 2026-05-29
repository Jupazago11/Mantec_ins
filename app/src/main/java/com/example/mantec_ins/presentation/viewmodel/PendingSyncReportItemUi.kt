package com.example.mantec_ins.presentation.viewmodel

data class PendingSyncReportItemUi(
    val reportLocalId: String,
    val clientName: String,
    val areaName: String,
    val elementName: String,
    val componentName: String,
    val diagnosticName: String,
    val status: String
)
