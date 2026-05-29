package com.example.mantec_ins.presentation.viewmodel

import com.example.mantec_ins.data.local.MeasurementThicknessDraftEntity
import com.example.mantec_ins.data.local.MeasurementThicknessDraftLineEntity
import com.example.mantec_ins.data.remote.MeasurementAreaDto
import com.example.mantec_ins.data.remote.MeasurementElementDto
import com.example.mantec_ins.data.remote.MeasurementElementTypeDto

data class MeasurementThicknessUiState(
    val isLoading: Boolean = false,
    val isSavingLocal: Boolean = false,
    val isSyncing: Boolean = false,

    val message: String? = null,
    val errorMessage: String? = null,

    val elementTypes: List<MeasurementElementTypeDto> = emptyList(),
    val areas: List<MeasurementAreaDto> = emptyList(),
    val elements: List<MeasurementElementDto> = emptyList(),

    val selectedElementTypeId: Long? = null,
    val selectedAreaId: Long? = null,
    val selectedElementId: Long? = null,

    val selectedElementTypeName: String = "",
    val selectedAreaName: String = "",
    val selectedElementName: String = "",

    val draft: MeasurementThicknessDraftEntity? = null,
    val lines: List<MeasurementThicknessDraftLineEntity> = emptyList(),

    val shouldShowElementTypeSelector: Boolean = false
)