package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteConditionDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("client_id")
    val clientId: Long,

    @SerializedName("element_type_id")
    val elementTypeId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("severity")
    val severity: Int,

    @SerializedName("color")
    val color: String? = null,

    @SerializedName("status")
    val status: Boolean = true
)