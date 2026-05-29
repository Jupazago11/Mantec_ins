package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MeasurementThicknessDao {

    @Query("""
        SELECT * FROM measurement_thickness_drafts
        WHERE elementId = :elementId
        LIMIT 1
    """)
    suspend fun getDraftByElement(elementId: Long): MeasurementThicknessDraftEntity?

    @Query("""
        SELECT * FROM measurement_thickness_draft_lines
        WHERE elementId = :elementId
        ORDER BY coverNumber ASC
    """)
    suspend fun getLinesByElement(elementId: Long): List<MeasurementThicknessDraftLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: MeasurementThicknessDraftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<MeasurementThicknessDraftLineEntity>)

    @Query("""
        DELETE FROM measurement_thickness_draft_lines
        WHERE elementId = :elementId
    """)
    suspend fun deleteLinesByElement(elementId: Long)

    @Transaction
    suspend fun replaceDraftWithLines(
        draft: MeasurementThicknessDraftEntity,
        lines: List<MeasurementThicknessDraftLineEntity>
    ) {
        upsertDraft(draft)
        deleteLinesByElement(draft.elementId)
        insertLines(lines)
    }

    @Query("""
        UPDATE measurement_thickness_drafts
        SET syncStatus = :syncStatus,
            lastError = :lastError
        WHERE elementId = :elementId
    """)
    suspend fun updateDraftSyncStatus(
        elementId: Long,
        syncStatus: String,
        lastError: String?
    )

    @Query("""
        SELECT * FROM measurement_thickness_drafts
        WHERE syncStatus IN ('PENDING_SYNC', 'CONFLICT', 'ERROR')
        ORDER BY localUpdatedAt DESC
    """)
    suspend fun getPendingDrafts(): List<MeasurementThicknessDraftEntity>

    @Query("""
    SELECT COUNT(*) FROM measurement_thickness_drafts
    WHERE syncStatus IN ('PENDING_SYNC', 'CONFLICT', 'ERROR')
""")
    suspend fun countPendingDrafts(): Int

    @Query("""
        DELETE FROM measurement_thickness_drafts
    """)
    suspend fun deleteAllDrafts()

    @Query("""
        DELETE FROM measurement_thickness_draft_lines
    """)
    suspend fun deleteAllLines()
}