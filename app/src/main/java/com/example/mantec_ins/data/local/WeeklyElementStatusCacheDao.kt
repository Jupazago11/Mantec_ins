package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeeklyElementStatusCacheDao {

    @Query("""
        DELETE FROM weekly_element_status_cache
        WHERE areaId = :areaId AND elementTypeId = :elementTypeId
    """)
    suspend fun deleteByAreaAndElementType(
        areaId: Long,
        elementTypeId: Long
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WeeklyElementStatusCacheEntity>)

    @Query("""
        SELECT * FROM weekly_element_status_cache
        WHERE areaId = :areaId
          AND elementTypeId = :elementTypeId
          AND week = :week
          AND year = :year
    """)
    suspend fun getByAreaAndElementTypeAndWeek(
        areaId: Long,
        elementTypeId: Long,
        week: Int,
        year: Int
    ): List<WeeklyElementStatusCacheEntity>
}