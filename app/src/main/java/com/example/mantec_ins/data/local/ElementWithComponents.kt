package com.example.mantec_ins.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ElementWithComponents(

    @Embedded val element: ElementEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ElementComponentCrossRef::class,
            parentColumn = "element_id",
            entityColumn = "component_id"
        )
    )
    val components: List<ComponentEntity>
)
