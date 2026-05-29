package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class RemoteElementDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("area_id")
    val areaId: Long,

    @SerializedName("group_id")
    val groupId: Long? = null,

    @SerializedName("element_type_id")
    val elementTypeId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String?,

    @SerializedName("warehouse_code")
    val warehouseCode: String?,

    @SerializedName("status")
    val status: Boolean
)