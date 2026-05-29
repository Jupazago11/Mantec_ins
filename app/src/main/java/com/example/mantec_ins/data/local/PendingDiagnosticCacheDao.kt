package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingDiagnosticCacheDao {

    @Query("DELETE FROM weekly_diagnostic_status_cache WHERE elementId = :elementId")
    suspend fun deleteByElement(elementId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WeeklyDiagnosticStatusCacheEntity>)

    @Query("""
        SELECT * FROM weekly_diagnostic_status_cache
        WHERE elementId = :elementId AND week = :week AND year = :year
    """)
    suspend fun getByElementAndWeek(
        elementId: Long,
        week: Int,
        year: Int
    ): List<WeeklyDiagnosticStatusCacheEntity>
}