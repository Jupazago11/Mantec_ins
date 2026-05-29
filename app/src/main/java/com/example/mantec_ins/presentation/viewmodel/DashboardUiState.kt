package com.example.mantec_ins.presentation.viewmodel

data class DashboardUiState(
    val pendingItems: List<PendingDiagnosticItemUi> = emptyList(),
    val weeklyDiagnosticItems: List<PendingDiagnosticItemUi> = emptyList(),
    val recentItems: List<RecentReportItemUi> = emptyList(),
    val weeklyElementStatuses: List<WeeklyElementStatusItemUi> = emptyList()
)