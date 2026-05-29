package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evidences")
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportLocalId: String,
    val reportDetailId: Long?,
    val localPath: String,
    val type: String,
    val syncStatus: String,
    val serverFileId: Long? = null
)
