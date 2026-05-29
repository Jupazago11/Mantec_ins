package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "element_types")
data class ElementTypeEntity(
    @PrimaryKey
    val id: Long,
    val clientId: Long,
    val name: String,
    val description: String?,
    val status: Boolean
)