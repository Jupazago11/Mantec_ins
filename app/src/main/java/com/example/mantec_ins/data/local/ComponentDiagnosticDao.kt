package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ComponentDiagnosticDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<ComponentDiagnosticCrossRef>)

    @Query("DELETE FROM component_diagnostic_cross_ref WHERE component_id = :componentId")
    suspend fun deleteByComponentId(componentId: Long)

    @Query("DELETE FROM component_diagnostic_cross_ref")
    suspend fun deleteAll()
}
