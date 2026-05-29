package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val obs: String?,
    val status: Boolean
)