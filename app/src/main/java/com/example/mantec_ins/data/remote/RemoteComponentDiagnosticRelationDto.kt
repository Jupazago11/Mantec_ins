package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteComponentDiagnosticRelationDto(
    @SerializedName("component_id")
    val componentId: Long,

    @SerializedName("diagnostic_id")
    val diagnosticId: Long
)
