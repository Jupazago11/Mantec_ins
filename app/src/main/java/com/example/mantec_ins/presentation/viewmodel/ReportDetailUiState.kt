package com.example.mantec_ins.presentation.viewmodel

import com.example.mantec_ins.data.local.EvidenceEntity

data class ReportDetailUiState(
    val selectedReportId: String? = null,
    val details: List<ReportDetailItemUi> = emptyList(),
    val evidences: List<EvidenceEntity> = emptyList()
)