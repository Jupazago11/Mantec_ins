package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteDiagnosticDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("client_id")
    val clientId: Long,

    @SerializedName("element_type_id")
    val elementTypeId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("status")
    val status: Boolean
)
