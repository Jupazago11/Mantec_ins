package com.example.mantec_ins.data.remote.personal

// Login del supervisor (Employee), separado del login del Inspector
// (User, ver LoginRequest/LoginResponse) — mismo backend, endpoint
// distinto (api/personal/login). Ver API_SUPERVISOR.md.

data class PersonalLoginRequest(
    val username: String,
    val password: String
)

data class PersonalLoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val employee: PersonalEmployeeDto?
)

data class PersonalEmployeeDto(
    val id: Long,
    val nombre: String,
    val nickname: String,
    val personal_role: PersonalRoleDto?
)

data class PersonalRoleDto(
    val id: Long,
    val name: String,
    val responsable_actividad: Boolean
)

data class PersonalLogoutResponse(
    val success: Boolean,
    val message: String
)
