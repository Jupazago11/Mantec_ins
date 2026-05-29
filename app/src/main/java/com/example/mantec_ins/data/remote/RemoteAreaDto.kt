package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteAreaDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("client_id")
    val clientId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("status")
    val status: Boolean
)
