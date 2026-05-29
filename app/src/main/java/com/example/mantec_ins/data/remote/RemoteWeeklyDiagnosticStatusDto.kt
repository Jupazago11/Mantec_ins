package com.example.mantec_ins.data.remote

data class RemoteWeeklyDiagnosticStatusDto(
    val component_id: Long,
    val component_name: String,
    val diagnostic_id: Long,
    val diagnostic_name: String,
    val status: String
)