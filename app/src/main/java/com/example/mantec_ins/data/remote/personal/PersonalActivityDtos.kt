package com.example.mantec_ins.data.remote.personal

// Actividades del supervisor — mismo shape que sirve
// Api\Personal\ActivityController::serialize() del backend Laravel (y
// que a su vez es el mismo que ya usaba la pantalla web "Ver como",
// secciones 14.18/14.20). snake_case calcado del JSON, mismo estilo que
// sync/SyncReportRequest.kt.

data class PersonalActivitiesResponse(
    val success: Boolean,
    val date: String,
    val actividades: List<PersonalActivityDto>
)

data class PersonalActivityDto(
    val id: Long,
    val date: String,
    val company_name: String,
    val team: String?,
    val process: String?,
    val description: String,
    val activity_type: String,
    val shift: String,
    val estimated_hours: Double?,
    val closed: Boolean,
    val comments: String?,
    val all_worked_scheduled_hours: Boolean?,
    val reported_hours: Double?,
    val registrado: Boolean,
    val personas: List<PersonalPersonaDto>,
    val evidencias: List<PersonalEvidenceDto>
)

data class PersonalPersonaDto(
    val id: Long,
    val nombre: String,
    val nickname: String,
    val worked_hours: Double?,
    val comment: String?
)

data class PersonalEvidenceDto(
    val id: Long,
    val original_name: String,
    val file_type: String, // "image" | "video"
    val show_url: String,
    val delete_url: String
)

// Body de POST api/personal/actividades/{id} — mismos campos que valida
// Api\Personal\ActivityController::validated() del lado Laravel.
data class PersonalSaveActivityRequest(
    val comments: String?,
    val all_worked_scheduled_hours: Boolean,
    val personas: List<PersonalPersonaHoursRequest>
)

data class PersonalPersonaHoursRequest(
    val employee_id: Long,
    val worked_hours: Double,
    val comment: String?
)

data class PersonalSaveActivityResponse(
    val success: Boolean,
    val message: String,
    val activity: PersonalActivityDto
)

// GET api/personal/actividades/{id}/evidencias/{evidenceId} — URL
// firmada de R2, vigente 10 minutos (ver
// Api\Personal\ActivityEvidenceController::show()).
data class PersonalEvidenceShowResponse(
    val success: Boolean,
    val url: String,
    val expires_at: String,
    val file_type: String,
    val original_name: String
)

data class PersonalEvidenceUploadResponse(
    val success: Boolean,
    val message: String,
    val evidencias: List<PersonalEvidenceDto>
)

data class PersonalGenericResponse(
    val success: Boolean,
    val message: String
)
