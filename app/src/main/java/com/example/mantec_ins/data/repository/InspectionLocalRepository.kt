package com.example.mantec_ins.data.repository

import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.EvidenceEntity
import com.example.mantec_ins.data.local.ReportDetailEntity
import com.example.mantec_ins.data.local.ReportEntity
import com.example.mantec_ins.presentation.viewmodel.PendingSyncReportItemUi
import com.example.mantec_ins.presentation.viewmodel.LocalPendingDiagnosticItemUi


class InspectionLocalRepository(
    private val database: AppDatabase
) {

    suspend fun saveReport(report: ReportEntity) {
        database.reportDao().insert(report)
    }

    suspend fun saveReportDetail(detail: ReportDetailEntity): Long {
        return database.reportDetailDao().insert(detail)
    }

    suspend fun getPendingSyncReportItems(): List<PendingSyncReportItemUi> {
        return database.reportDao().getPendingSyncReportItems()
    }

    suspend fun saveEvidence(evidence: EvidenceEntity): Long {
        return database.evidenceDao().insert(evidence)
    }

    suspend fun getAllReports(): List<ReportEntity> {
        return database.reportDao().getAll()
    }

    suspend fun getPendingReports(): List<ReportEntity> {
        return database.reportDao().getByStatus("PENDING_SYNC")
    }

    suspend fun getLocalPendingDiagnosticsForWeek(
        week: Int,
        year: Int
    ): List<LocalPendingDiagnosticItemUi> {
        return database.reportDetailDao().getLocalPendingDiagnosticsForWeek(
            week = week,
            year = year
        )
    }

    suspend fun getReportDetails(reportLocalId: String): List<ReportDetailEntity> {
        return database.reportDetailDao().getByReport(reportLocalId)
    }

    suspend fun getRecentReportDetailsFromDate(fromDate: String): List<ReportDetailEntity> {
        return database.reportDetailDao().getRecentFromDate(fromDate)
    }

    suspend fun getReportEvidences(reportLocalId: String): List<EvidenceEntity> {
        return database.evidenceDao().getByReport(reportLocalId)
    }

    suspend fun getAllReportDetails(): List<ReportDetailEntity> {
        return database.reportDetailDao().getRecentFromDate("0000-01-01")
    }

    suspend fun clearAllInspectionData() {
        database.evidenceDao().deleteAll()
        database.reportDetailDao().deleteAll()
        database.reportDao().deleteAll()
    }
}