package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conditions")
data class ConditionEntity(
    @PrimaryKey
    val id: Long,
    val clientId: Long,
    val elementTypeId: Long,
    val name: String,
    val code: String,
    val description: String?,
    val severity: Int,
    val color: String?,
    val status: Boolean
)