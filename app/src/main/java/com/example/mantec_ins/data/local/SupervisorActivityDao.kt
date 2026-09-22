package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SupervisorActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: SupervisorActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<SupervisorActivityEntity>)

    @Query("SELECT * FROM supervisor_activities ORDER BY date DESC, id")
    suspend fun getAll(): List<SupervisorActivityEntity>

    @Query("SELECT * FROM supervisor_activities WHERE id = :id")
    suspend fun getById(id: Long): SupervisorActivityEntity?

    @Query("SELECT * FROM supervisor_activities WHERE registrationSyncStatus = :status")
    suspend fun getByStatus(status: String): List<SupervisorActivityEntity>

    @Query("""
        UPDATE supervisor_activities
        SET comments = :comments, allWorkedScheduledHours = :allWorkedScheduledHours,
            reportedHours = :reportedHours, registrado = :registrado,
            registrationSyncStatus = :registrationSyncStatus, lastError = :lastError
        WHERE id = :id
    """)
    suspend fun updateRegistration(
        id: Long,
        comments: String?,
        allWorkedScheduledHours: Boolean?,
        reportedHours: Double?,
        registrado: Boolean,
        registrationSyncStatus: String,
        lastError: String? = null
    )

    @Query("UPDATE supervisor_activities SET registrationSyncStatus = :status, lastError = :lastError WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, lastError: String? = null)

    // Usado por el refresh/merge: solo pisa los campos de solo-lectura,
    // nunca comments/allWorkedScheduledHours/reportedHours/registrado —
    // esos los controla el registro local mientras haya un
    // registrationSyncStatus = PENDING_SYNC sin enviar (ver
    // SupervisorSyncRepository).
    @Query("""
        UPDATE supervisor_activities
        SET date = :date, companyName = :companyName, team = :team, process = :process,
            description = :description, activityType = :activityType, shift = :shift,
            estimatedHours = :estimatedHours, closed = :closed
        WHERE id = :id
    """)
    suspend fun updateReadOnlyFields(
        id: Long,
        date: String,
        companyName: String,
        team: String?,
        process: String?,
        description: String,
        activityType: String,
        shift: String,
        estimatedHours: Double?,
        closed: Boolean
    )

    @Query("DELETE FROM supervisor_activities")
    suspend fun deleteAll()
}
