package com.example.mantec_ins.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ComponentWithConditions(
    @Embedded
    val component: ComponentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ComponentConditionCrossRef::class,
            parentColumn = "component_id",
            entityColumn = "condition_id"
        )
    )
    val conditions: List<ConditionEntity>
)