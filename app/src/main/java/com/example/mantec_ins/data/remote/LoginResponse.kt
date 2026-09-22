package com.example.mantec_ins.data.remote

import com.example.mantec_ins.data.remote.personal.PersonalEmployeeDto

// Login unico (ver LOGIN_Y_ROLES.md): api/login devuelve "user" si las
// credenciales matchearon un User (Inspector/Admin) o "employee" si
// matchearon un Employee (Supervisor) — nunca ambos a la vez.
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: LoginUserDto?,
    val employee: PersonalEmployeeDto? = null
)

data class LoginUserDto(
    val id: Long,
    val name: String,
    val username: String,
    val email: String?,
    val role: LoginRoleDto?,
    val clients: List<LoginClientDto>,
    val allowed_element_types: List<LoginElementTypeDto>
)

data class LoginRoleDto(
    val id: Long?,
    val name: String?,
    val key: String?
)

data class LoginClientDto(
    val id: Long,
    val name: String
)

data class LoginElementTypeDto(
    val id: Long,
    val name: String,
    val client_id: Long
)
