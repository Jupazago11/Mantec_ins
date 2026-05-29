package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_details")
data class ReportDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportLocalId: String,
    val componentId: Long,
    val diagnosticId: Long,
    val conditionId: Long,
    val recommendation: String?,
    val week: Int,
    val year: Int,
    val executionDate: String,
    val syncStatus: String,
    val isBeltChange: Boolean = false,
    val serverId: Long? = null
)
