package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CatalogRelationDao {

    @Transaction
    @Query("SELECT * FROM elements WHERE id = :elementId")
    suspend fun getElementWithComponents(elementId: Long): ElementWithComponents?

    @Transaction
    @Query("SELECT * FROM components WHERE id = :componentId")
    suspend fun getComponentWithDiagnostics(componentId: Long): ComponentWithDiagnostics?

    @Transaction
    @Query("SELECT * FROM components WHERE id = :componentId")
    suspend fun getComponentWithConditions(componentId: Long): ComponentWithConditions?
}