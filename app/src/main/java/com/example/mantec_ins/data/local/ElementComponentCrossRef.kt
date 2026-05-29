package com.example.mantec_ins.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "element_component_cross_ref",
    primaryKeys = ["element_id", "component_id"]
)
data class ElementComponentCrossRef(

    @ColumnInfo(name = "element_id")
    val elementId: Long,

    @ColumnInfo(name = "component_id")
    val componentId: Long
)
