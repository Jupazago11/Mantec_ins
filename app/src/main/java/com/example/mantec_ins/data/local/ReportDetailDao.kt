package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mantec_ins.presentation.viewmodel.LocalPendingDiagnosticItemUi

@Dao
interface ReportDetailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: ReportDetailEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<ReportDetailEntity>)

    @Query("SELECT * FROM report_details WHERE reportLocalId = :reportLocalId")
    suspend fun getByReport(reportLocalId: String): List<ReportDetailEntity>

    @Query("SELECT * FROM report_details WHERE reportLocalId = :reportLocalId LIMIT 1")
    suspend fun getSingleByReport(reportLocalId: String): ReportDetailEntity?

    @Query("""
        UPDATE report_details
        SET serverId = :serverId, syncStatus = :syncStatus
        WHERE reportLocalId = :reportLocalId
    """)
    suspend fun updateSyncData(
        reportLocalId: String,
        serverId: Long,
        syncStatus: String
    )

    @Query("""
        SELECT * FROM report_details
        WHERE executionDate >= :fromDate
        ORDER BY executionDate DESC, id DESC
    """)
    suspend fun getRecentFromDate(fromDate: String): List<ReportDetailEntity>

    @Query("""
    SELECT
        r.elementId AS elementId,
        rd.componentId AS componentId,
        rd.diagnosticId AS diagnosticId
    FROM report_details rd
    INNER JOIN reports r ON r.localId = rd.reportLocalId
    WHERE rd.week = :week
      AND rd.year = :year
      AND (
          r.status != 'SYNCED'
          OR rd.syncStatus != 'SYNCED'
      )
""")
    suspend fun getLocalPendingDiagnosticsForWeek(
        week: Int,
        year: Int
    ): List<LocalPendingDiagnosticItemUi>

    @Query("DELETE FROM report_details")
    suspend fun deleteAll()
}
