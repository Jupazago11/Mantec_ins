package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteComponentDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("client_id")
    val clientId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String?,

    @SerializedName("element_type_id")
    val elementTypeId: Long,

    @SerializedName("is_required")
    val isRequired: Boolean,

    @SerializedName("is_default")
    val isDefault: Boolean,

    @SerializedName("status")
    val status: Boolean
)
