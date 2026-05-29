package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "weekly_diagnostic_status_cache",
    primaryKeys = ["elementId", "componentId", "diagnosticId", "week", "year"],
    indices = [
        Index(value = ["elementId"]),
        Index(value = ["componentId"]),
        Index(value = ["diagnosticId"])
    ]
)
data class WeeklyDiagnosticStatusCacheEntity(
    val elementId: Long,
    val componentId: Long,
    val diagnosticId: Long,
    val diagnosticName: String,
    val componentName: String,
    val status: String,
    val week: Int,
    val year: Int,
    val updatedAt: Long = System.currentTimeMillis()
)