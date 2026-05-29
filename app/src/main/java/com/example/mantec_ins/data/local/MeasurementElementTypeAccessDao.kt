package com.example.mantec_ins.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MeasurementElementTypeAccessDao {

    @Query("""
        SELECT * FROM measurement_element_type_access
        WHERE moduleEnabled = 1
          AND creationEnabled = 1
          AND status = 1
        ORDER BY name ASC
    """)
    suspend fun getEnabledForCreation(): List<MeasurementElementTypeAccessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MeasurementElementTypeAccessEntity>)

    @Query("DELETE FROM measurement_element_type_access")
    suspend fun deleteAll()
}