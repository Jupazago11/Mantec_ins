package com.example.mantec_ins.data.local

import androidx.room.Entity

// FK textual a SupervisorActivityEntity.id (sin @ForeignKey declarado,
// mismo estilo de join manual que ReportDetailEntity/EvidenceEntity).
// PK compuesta (no autoGenerate): una persona aparece una sola vez por
// actividad, asi que un upsert por REPLACE reemplaza limpio en cada
// refresh/edicion sin acumular duplicados.
@Entity(tableName = "supervisor_personas", primaryKeys = ["activityId", "employeeId"])
data class SupervisorPersonaEntity(
    val activityId: Long,
    val employeeId: Long,
    val nombre: String,
    val nickname: String,
    val workedHours: Double?,
    val comentario: String? = null
)
