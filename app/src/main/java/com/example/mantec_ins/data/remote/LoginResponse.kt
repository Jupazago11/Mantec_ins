package com.example.mantec_ins.data.remote

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: LoginUserDto?
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
