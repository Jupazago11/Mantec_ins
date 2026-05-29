package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_element_type_access")
data class MeasurementElementTypeAccessEntity(
    @PrimaryKey
    val elementTypeId: Long,
    val clientId: Long,
    val name: String,
    val description: String?,
    val moduleEnabled: Boolean,
    val creationEnabled: Boolean,
    val status: Boolean
)