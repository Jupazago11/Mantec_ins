package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ComponentDao {

    @Query("SELECT * FROM components ORDER BY name ASC")
    suspend fun getAll(): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE clientId = :clientId ORDER BY name ASC")
    suspend fun getByClient(clientId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE elementTypeId = :elementTypeId ORDER BY name ASC")
    suspend fun getByElementType(elementTypeId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE clientId = :clientId AND elementTypeId = :elementTypeId ORDER BY name ASC")
    suspend fun getByClientAndElementType(clientId: Long, elementTypeId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE id = :componentId LIMIT 1")
    suspend fun getById(componentId: Long): ComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(components: List<ComponentEntity>)

    @Query("DELETE FROM components")
    suspend fun deleteAll()
}