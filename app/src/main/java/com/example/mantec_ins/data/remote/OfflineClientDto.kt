package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class OfflineClientDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("obs")
    val obs: String?,

    @SerializedName("status")
    val status: Boolean
)
