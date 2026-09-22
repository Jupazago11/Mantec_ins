package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Offline-first del Supervisor (ver API_SUPERVISOR.md / OFFLINE_SUPERVISOR.md):
// a diferencia de ReportEntity (creado 100% en el cliente, con un UUID local
// como PK), la actividad YA existe en el servidor desde que Programacion la
// crea — la app del supervisor solo la lee y le agrega registro. Por eso la
// PK es el id real del servidor, no un UUID.
@Entity(tableName = "supervisor_activities")
data class SupervisorActivityEntity(
    @PrimaryKey
    val id: Long,
    val date: String,
    val companyName: String,
    val team: String?,
    val process: String?,
    val description: String,
    val activityType: String,
    val shift: String,
    val estimatedHours: Double?,
    val closed: Boolean,

    // Campos editables por el supervisor. Cuando registrationSyncStatus es
    // PENDING_SYNC, estos valores son la edicion LOCAL sin enviar todavia —
    // un refresh desde el servidor no debe pisarlos (ver
    // SupervisorSyncRepository).
    val comments: String?,
    val allWorkedScheduledHours: Boolean?,
    val reportedHours: Double?,
    val registrado: Boolean,

    // SYNCED | PENDING_SYNC | ERROR — ver PATRONES_ASINCRONISMO_OFFLINE.md
    // patron 2. ERROR es terminal (403 actividad cerrada, 422 validacion) y
    // ya no se reintenta solo; PENDING_SYNC es transitorio y se reintenta en
    // cada ciclo de sync.
    val registrationSyncStatus: String,
    val lastError: String? = null
)
