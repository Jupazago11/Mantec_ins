package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostics")
data class DiagnosticEntity(
    @PrimaryKey
    val id: Long,
    val clientId: Long,
    val elementTypeId: Long,
    val name: String,
    val description: String?,
    val status: Boolean
)