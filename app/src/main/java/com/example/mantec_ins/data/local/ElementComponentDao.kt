package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ElementComponentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<ElementComponentCrossRef>)

    @Query("DELETE FROM element_component_cross_ref WHERE element_id = :elementId")
    suspend fun deleteByElementId(elementId: Long)

    @Query("DELETE FROM element_component_cross_ref")
    suspend fun deleteAll()
}
