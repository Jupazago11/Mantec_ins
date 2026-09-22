package com.example.mantec_ins.data.repository

import com.example.mantec_ins.data.local.SessionManager
import com.example.mantec_ins.data.remote.AuthApiService
import com.example.mantec_ins.data.remote.LoginRequest
import com.example.mantec_ins.data.remote.personal.PersonalApiService
import com.example.mantec_ins.domain.model.UserSession

// Login unico Inspector/Supervisor (ver LOGIN_Y_ROLES.md): un solo
// endpoint (api/login) que en el backend prueba User y, si no matchea,
// Employee — aca solo hace falta leer cual de los dos vino en la
// respuesta. personalApi se usa unicamente para el logout remoto de una
// sesion de Supervisor (api/personal/logout), que sigue siendo un
// endpoint aparte del de Inspector (api/logout).
class AuthRepository(
    private val apiService: AuthApiService,
    private val personalApi: PersonalApiService,
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

            if (!response.success || response.token.isNullOrBlank()) {
                Result.failure(Exception(response.message))
            } else if (response.user != null) {
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
            } else if (response.employee != null) {
                val employee = response.employee

                val session = UserSession(
                    userId = employee.id,
                    userName = employee.nombre,
                    username = employee.nickname,
                    roleKey = "supervisor",
                    clientId = null,
                    clientName = null,
                    elementTypeId = null,
                    elementTypeName = null,
                    token = response.token
                )

                sessionManager.saveSession(session)
                Result.success(session)
            } else {
                Result.failure(Exception(response.message))
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

    // Suspend porque una sesion de Supervisor necesita cerrar sesion
    // tambien del lado del servidor (api/personal/logout) antes de
    // limpiar la sesion local — el token puede ya estar vencido/invalido,
    // eso no le importa al usuario (solo quiere cerrar sesion en su
    // celular), de ahi el try/catch silencioso.
    suspend fun logout() {
        val roleKey = sessionManager.getSession()?.roleKey
        if (roleKey == "supervisor") {
            try {
                personalApi.logout()
            } catch (e: Exception) {
                // ignorado a proposito, ver comentario arriba
            }
        }
        sessionManager.clearSession()
    }
}
