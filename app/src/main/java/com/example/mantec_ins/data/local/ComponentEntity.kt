package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey
    val id: Long,
    val clientId: Long,
    val name: String,
    val code: String?,
    val elementTypeId: Long,
    val isRequired: Boolean,
    val isDefault: Boolean,
    val status: Boolean
)