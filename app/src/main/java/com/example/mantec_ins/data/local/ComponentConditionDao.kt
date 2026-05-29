package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ComponentConditionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ComponentConditionCrossRef>)

    @Query("DELETE FROM component_condition_cross_ref")
    suspend fun deleteAll()

    @Query("DELETE FROM component_condition_cross_ref WHERE component_id = :componentId")
    suspend fun deleteByComponentId(componentId: Long)
}