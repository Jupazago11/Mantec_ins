package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_thickness_draft_lines")
data class MeasurementThicknessDraftLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val elementId: Long,

    val remoteLineId: Long?,

    val coverNumber: Int,

    val topLeft: Double?,
    val topCenter: Double?,
    val topRight: Double?,

    val bottomLeft: Double?,
    val bottomCenter: Double?,
    val bottomRight: Double?,

    val hardnessLeft: Double?,
    val hardnessCenter: Double?,
    val hardnessRight: Double?
)