package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val localId: String,
    val clientId: Long,
    val areaId: Long,
    val elementId: Long,
    val userId: Long,
    val createdAt: String,
    val status: String
)