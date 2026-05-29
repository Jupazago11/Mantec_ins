package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GroupDao {

    @Query("SELECT * FROM groups ORDER BY name ASC")
    suspend fun getAll(): List<GroupEntity>

    @Query("SELECT * FROM groups LIMIT 1")
    suspend fun getFirst(): GroupEntity?

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getById(groupId: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()
}