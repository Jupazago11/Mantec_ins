package com.example.mantec_ins.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_thickness_drafts")
data class MeasurementThicknessDraftEntity(
    @PrimaryKey
    val elementId: Long,

    val remoteDraftId: Long?,

    val clientId: Long,
    val areaId: Long,
    val elementTypeId: Long,

    val createdBy: Long?,
    val updatedBy: Long?,

    val remoteCreatedAt: String?,
    val remoteUpdatedAt: String?,

    val localUpdatedAt: Long,

    /**
     * SYNCED:
     *   el borrador local está igual al backend.
     *
     * PENDING_SYNC:
     *   el inspector creó/editó localmente y falta sincronizar.
     *
     * CONFLICT:
     *   el backend rechazó por conflicto de actualización.
     *
     * ERROR:
     *   falló la sincronización por error no controlado.
     */
    val syncStatus: String,

    val lastError: String? = null
)