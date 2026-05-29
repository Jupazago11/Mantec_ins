package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteGroupDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("client_id")
    val clientId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("auto_sync")
    val autoSync: Boolean,

    @SerializedName("status")
    val status: Boolean
)