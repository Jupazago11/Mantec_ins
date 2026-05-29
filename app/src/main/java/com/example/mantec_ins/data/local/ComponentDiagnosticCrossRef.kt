package com.example.mantec_ins.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "component_diagnostic_cross_ref",
    primaryKeys = ["component_id", "diagnostic_id"]
)
data class ComponentDiagnosticCrossRef(

    @ColumnInfo(name = "component_id")
    val componentId: Long,

    @ColumnInfo(name = "diagnostic_id")
    val diagnosticId: Long
)
