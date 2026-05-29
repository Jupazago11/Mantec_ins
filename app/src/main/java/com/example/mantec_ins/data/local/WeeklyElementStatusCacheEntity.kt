package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "weekly_element_status_cache",
    primaryKeys = ["areaId", "elementTypeId", "elementId", "week", "year"],
    indices = [
        Index(value = ["areaId"]),
        Index(value = ["elementTypeId"]),
        Index(value = ["elementId"])
    ]
)
data class WeeklyElementStatusCacheEntity(
    val areaId: Long,
    val elementTypeId: Long,
    val elementId: Long,
    val elementName: String,
    val status: String,
    val expectedCount: Int,
    val doneCount: Int,
    val week: Int,
    val year: Int,
    val updatedAt: Long = System.currentTimeMillis()
)