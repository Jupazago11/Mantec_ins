package com.example.mantec_ins.sync

data class SyncReportRequest(
    val local_report_id: String,
    val client_id: Long,
    val area_id: Long,
    val element_id: Long,
    val component_id: Long,
    val diagnostic_id: Long,
    val condition_id: Long,
    val recommendation: String?,
    val week: Int,
    val year: Int,
    val execution_date: String,
    val is_belt_change: Boolean?
)
