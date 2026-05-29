package com.example.mantec_ins.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ComponentWithDiagnostics(

    @Embedded val component: ComponentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ComponentDiagnosticCrossRef::class,
            parentColumn = "component_id",
            entityColumn = "diagnostic_id"
        )
    )
    val diagnostics: List<DiagnosticEntity>
)
