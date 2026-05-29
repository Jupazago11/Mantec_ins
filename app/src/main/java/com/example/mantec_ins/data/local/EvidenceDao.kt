package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EvidenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: EvidenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evidences: List<EvidenceEntity>)

    @Query("SELECT * FROM evidences WHERE reportLocalId = :reportLocalId")
    suspend fun getByReport(reportLocalId: String): List<EvidenceEntity>

    @Query("""
        UPDATE evidences
        SET syncStatus = :syncStatus, serverFileId = :serverFileId
        WHERE id = :id
    """)
    suspend fun updateSyncData(
        id: Long,
        syncStatus: String,
        serverFileId: Long
    )

    @Query("DELETE FROM evidences")
    suspend fun deleteAll()
}
