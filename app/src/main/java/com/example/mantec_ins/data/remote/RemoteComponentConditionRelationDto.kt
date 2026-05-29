package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteComponentConditionRelationDto(
    @SerializedName("component_id")
    val componentId: Long,

    @SerializedName("condition_id")
    val conditionId: Long
)