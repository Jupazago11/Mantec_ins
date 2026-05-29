package com.example.mantec_ins.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "component_condition_cross_ref",
    primaryKeys = ["component_id", "condition_id"],
    indices = [
        Index(value = ["component_id"]),
        Index(value = ["condition_id"])
    ]
)
data class ComponentConditionCrossRef(
    @ColumnInfo(name = "component_id")
    val componentId: Long,

    @ColumnInfo(name = "condition_id")
    val conditionId: Long
)