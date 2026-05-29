package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClientDao {

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAll(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<ClientEntity>)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}