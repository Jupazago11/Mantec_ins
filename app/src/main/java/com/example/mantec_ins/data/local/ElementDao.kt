package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ElementDao {

    @Query("SELECT * FROM elements ORDER BY name ASC")
    suspend fun getAll(): List<ElementEntity>

    @Query("SELECT * FROM elements WHERE areaId = :areaId ORDER BY name ASC")
    suspend fun getByArea(areaId: Long): List<ElementEntity>

    @Query("SELECT * FROM elements WHERE elementTypeId = :elementTypeId ORDER BY name ASC")
    suspend fun getByElementType(elementTypeId: Long): List<ElementEntity>

    @Query("SELECT * FROM elements WHERE groupId = :groupId ORDER BY name ASC")
    suspend fun getByGroup(groupId: Long): List<ElementEntity>

    @Query("SELECT * FROM elements WHERE areaId = :areaId AND elementTypeId = :elementTypeId ORDER BY name ASC")
    suspend fun getByAreaAndType(areaId: Long, elementTypeId: Long): List<ElementEntity>

    @Query("SELECT * FROM elements WHERE id = :elementId LIMIT 1")
    suspend fun getById(elementId: Long): ElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elements: List<ElementEntity>)

    @Query("DELETE FROM elements")
    suspend fun deleteAll()
}