package com.example.mantec_ins.domain.model

data class UserSession(
    val userId: Long,
    val userName: String,
    val username: String,
    val roleKey: String?,
    val clientId: Long?,
    val clientName: String?,
    val elementTypeId: Long?,
    val elementTypeName: String?,
    val token: String
)
