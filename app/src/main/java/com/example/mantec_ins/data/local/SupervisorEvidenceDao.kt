package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SupervisorEvidenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: SupervisorEvidenceEntity): Long

    @Query("SELECT * FROM supervisor_evidences WHERE activityId = :activityId")
    suspend fun getByActivity(activityId: Long): List<SupervisorEvidenceEntity>

    @Query("SELECT * FROM supervisor_evidences WHERE id = :id")
    suspend fun getById(id: Long): SupervisorEvidenceEntity?

    @Query("SELECT * FROM supervisor_evidences WHERE syncStatus = :status")
    suspend fun getByStatus(status: String): List<SupervisorEvidenceEntity>

    @Query("SELECT serverId FROM supervisor_evidences WHERE activityId = :activityId AND serverId IS NOT NULL")
    suspend fun getKnownServerIds(activityId: Long): List<Long>

    @Query("UPDATE supervisor_evidences SET syncStatus = :syncStatus, serverId = :serverId WHERE id = :id")
    suspend fun updateSyncData(id: Long, syncStatus: String, serverId: Long)

    @Query("UPDATE supervisor_evidences SET syncStatus = :syncStatus, lastError = :lastError WHERE id = :id")
    suspend fun updateStatus(id: Long, syncStatus: String, lastError: String? = null)

    @Query("DELETE FROM supervisor_evidences WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM supervisor_evidences")
    suspend fun deleteAll()
}
