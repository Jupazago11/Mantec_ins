package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AreaDao {

    @Query("SELECT * FROM areas ORDER BY name ASC")
    suspend fun getAll(): List<AreaEntity>

    @Query("SELECT * FROM areas WHERE clientId = :clientId ORDER BY name ASC")
    suspend fun getByClient(clientId: Long): List<AreaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(areas: List<AreaEntity>)

    @Query("DELETE FROM areas")
    suspend fun deleteAll()
}