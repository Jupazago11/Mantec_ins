package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Calco casi exacto de EvidenceEntity (Inspector), con una diferencia
// consciente: localPath apunta a una copia PROPIA del archivo en
// almacenamiento privado de la app (context.filesDir), no a la URI
// publica de MediaStore tal cual — evita el riesgo silencioso ya
// detectado en el patron del Inspector (si el sistema limpia
// Pictures/ManTec antes del sync, la evidencia queda huerfana sin pasar
// nunca a ERROR). Ver OFFLINE_SUPERVISOR.md.
@Entity(tableName = "supervisor_evidences")
data class SupervisorEvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: Long,
    val localPath: String,       // path de archivo propio (filesDir), no content:// URI
    val originalName: String,
    val fileType: String,        // "image" | "video"
    val syncStatus: String,      // PENDING_SYNC | SYNCED | ERROR
    val serverId: Long? = null,
    val lastError: String? = null
)
