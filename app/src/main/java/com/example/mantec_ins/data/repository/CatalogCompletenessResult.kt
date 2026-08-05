package com.example.mantec_ins.data.repository

data class CatalogComponentGap(
    val componentId: Long,
    val componentName: String,
    val elementName: String,
    val missingDiagnostics: Boolean,
    val missingConditions: Boolean
)

data class CatalogCompletenessResult(
    val isComplete: Boolean,
    val gaps: List<CatalogComponentGap>
)
