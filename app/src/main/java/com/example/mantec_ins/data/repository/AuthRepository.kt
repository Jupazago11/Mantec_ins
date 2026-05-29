package com.example.mantec_ins.data.repository

import com.example.mantec_ins.data.local.SessionManager
import com.example.mantec_ins.data.remote.AuthApiService
import com.example.mantec_ins.data.remote.LoginRequest
import com.example.mantec_ins.domain.model.UserSession

class AuthRepository(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
) {

    suspend fun login(username: String, password: String): Result<UserSession> {
        return try {
            val response = apiService.login(
                LoginRequest(
                    username = username,
                    password = password
                )
            )

            if (!response.success || response.user == null || response.token.isNullOrBlank()) {
                Result.failure(Exception(response.message))
            } else {
                val user = response.user
                val firstClient = user.clients.firstOrNull()
                val firstElementType = user.allowed_element_types.firstOrNull()

                val session = UserSession(
                    userId = user.id,
                    userName = user.name,
                    username = user.username,
                    roleKey = user.role?.key,
                    clientId = firstClient?.id,
                    clientName = firstClient?.name,
                    elementTypeId = firstElementType?.id,
                    elementTypeName = firstElementType?.name,
                    token = response.token
                )

                sessionManager.saveSession(session)
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSavedSession(): UserSession? {
        return sessionManager.getSession()
    }

    fun getToken(): String? {
        return sessionManager.getToken()
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
