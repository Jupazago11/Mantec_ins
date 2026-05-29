package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ElementTypeDao {

    @Query("SELECT * FROM element_types ORDER BY name ASC")
    suspend fun getAll(): List<ElementTypeEntity>

    @Query("SELECT * FROM element_types WHERE clientId = :clientId ORDER BY name ASC")
    suspend fun getByClient(clientId: Long): List<ElementTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elementTypes: List<ElementTypeEntity>)

    @Query("DELETE FROM element_types")
    suspend fun deleteAll()
}