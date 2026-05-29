package com.example.mantec_ins.presentation.viewmodel

import com.example.mantec_ins.data.local.AreaEntity
import com.example.mantec_ins.data.local.ComponentEntity
import com.example.mantec_ins.data.local.ConditionEntity
import com.example.mantec_ins.data.local.DiagnosticEntity
import com.example.mantec_ins.data.local.ElementEntity

data class RemoteCatalogUiState(
    val areas: List<AreaEntity> = emptyList(),
    val conditions: List<ConditionEntity> = emptyList(),
    val elements: List<ElementEntity> = emptyList(),
    val components: List<ComponentEntity> = emptyList(),
    val diagnostics: List<DiagnosticEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
