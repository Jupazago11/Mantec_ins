package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SupervisorPersonaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personas: List<SupervisorPersonaEntity>)

    @Query("SELECT * FROM supervisor_personas WHERE activityId = :activityId")
    suspend fun getByActivity(activityId: Long): List<SupervisorPersonaEntity>

    @Query("UPDATE supervisor_personas SET workedHours = :workedHours WHERE activityId = :activityId AND employeeId = :employeeId")
    suspend fun updateWorkedHours(activityId: Long, employeeId: Long, workedHours: Double?)

    @Query("UPDATE supervisor_personas SET comentario = :comentario WHERE activityId = :activityId AND employeeId = :employeeId")
    suspend fun updateComentario(activityId: Long, employeeId: Long, comentario: String?)

    @Query("DELETE FROM supervisor_personas WHERE activityId = :activityId")
    suspend fun deleteByActivity(activityId: Long)

    @Query("DELETE FROM supervisor_personas")
    suspend fun deleteAll()
}
