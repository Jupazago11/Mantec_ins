package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConditionDao {

    @Query("SELECT * FROM conditions ORDER BY severity ASC, name ASC")
    suspend fun getAll(): List<ConditionEntity>

    @Query("""
        SELECT * FROM conditions
        WHERE clientId = :clientId
        ORDER BY severity ASC, name ASC
    """)
    suspend fun getByClient(clientId: Long): List<ConditionEntity>

    @Query("""
        SELECT * FROM conditions
        WHERE clientId = :clientId
          AND elementTypeId = :elementTypeId
        ORDER BY severity ASC, name ASC
    """)
    suspend fun getByClientAndElementType(
        clientId: Long,
        elementTypeId: Long
    ): List<ConditionEntity>

    @Query("SELECT * FROM conditions WHERE id = :conditionId LIMIT 1")
    suspend fun getById(conditionId: Long): ConditionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conditions: List<ConditionEntity>)

    @Query("DELETE FROM conditions")
    suspend fun deleteAll()
}
