package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: Long,
    val clientId: Long,
    val name: String,
    val description: String?,
    val autoSync: Boolean,
    val status: Boolean
)