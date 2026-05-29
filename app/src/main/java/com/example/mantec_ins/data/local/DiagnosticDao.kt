package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DiagnosticDao {

    @Query("SELECT * FROM diagnostics ORDER BY name ASC")
    suspend fun getAll(): List<DiagnosticEntity>

    @Query("SELECT * FROM diagnostics WHERE clientId = :clientId ORDER BY name ASC")
    suspend fun getByClient(clientId: Long): List<DiagnosticEntity>

    @Query("SELECT * FROM diagnostics WHERE id = :diagnosticId LIMIT 1")
    suspend fun getById(diagnosticId: Long): DiagnosticEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(diagnostics: List<DiagnosticEntity>)

    @Query("DELETE FROM diagnostics")
    suspend fun deleteAll()
}