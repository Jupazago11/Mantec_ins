package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mantec_ins.presentation.viewmodel.PendingSyncReportItemUi

@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getByStatus(status: String): List<ReportEntity>

    @Query("UPDATE reports SET status = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)

    @Query("""
        SELECT
            r.localId AS reportLocalId,
            c.name AS clientName,
            a.name AS areaName,
            e.name AS elementName,
            cp.name AS componentName,
            d.name AS diagnosticName,
            r.status AS status
        FROM reports r
        INNER JOIN clients c ON c.id = r.clientId
        INNER JOIN areas a ON a.id = r.areaId
        INNER JOIN elements e ON e.id = r.elementId
        INNER JOIN report_details rd ON rd.reportLocalId = r.localId
        INNER JOIN components cp ON cp.id = rd.componentId
        INNER JOIN diagnostics d ON d.id = rd.diagnosticId
        WHERE r.status != 'SYNCED'
        ORDER BY r.createdAt DESC
    """)
    suspend fun getPendingSyncReportItems(): List<PendingSyncReportItemUi>

    @Query("DELETE FROM reports")
    suspend fun deleteAll()
}
