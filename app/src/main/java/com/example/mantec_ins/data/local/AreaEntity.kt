package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "areas")
data class AreaEntity(
    @PrimaryKey
    val id: Long,
    val clientId: Long,
    val name: String,
    val code: String?,
    val status: Boolean
)