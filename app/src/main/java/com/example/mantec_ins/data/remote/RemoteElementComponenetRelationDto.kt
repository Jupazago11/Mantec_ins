package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteElementComponentRelationDto(
    @SerializedName("element_id")
    val elementId: Long,

    @SerializedName("component_id")
    val componentId: Long
)
