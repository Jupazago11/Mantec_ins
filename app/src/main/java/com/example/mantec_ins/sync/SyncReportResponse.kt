package com.example.mantec_ins.sync
data class SyncReportResponse(
    val success: Boolean,
    val message: String,
    val server_report_detail_id: Long,
    val duplicated: Boolean
)
