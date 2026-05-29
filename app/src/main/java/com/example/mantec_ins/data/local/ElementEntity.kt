package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "elements")
data class ElementEntity(
    @PrimaryKey
    val id: Long,
    val areaId: Long,
    val elementTypeId: Long,
    val name: String,
    val code: String?,
    val warehouseCode: String?,
    val status: Boolean,
    val groupId: Long? = null
)