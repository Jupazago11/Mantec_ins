package com.example.mantec_ins.data.local

import android.content.Context
import com.example.mantec_ins.domain.model.UserSession

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("mantec_session", Context.MODE_PRIVATE)

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putLong("user_id", session.userId)
            .putString("user_name", session.userName)
            .putString("username", session.username)
            .putString("role_key", session.roleKey)
            .putLong("client_id", session.clientId ?: -1L)
            .putString("client_name", session.clientName)
            .putLong("element_type_id", session.elementTypeId ?: -1L)
            .putString("element_type_name", session.elementTypeName)
            .putString("token", session.token)
            .apply()
    }

    fun getSession(): UserSession? {
        val userId = prefs.getLong("user_id", -1L)
        val token = prefs.getString("token", null)

        if (userId == -1L || token.isNullOrBlank()) return null

        val clientId = prefs.getLong("client_id", -1L)
        val elementTypeId = prefs.getLong("element_type_id", -1L)

        return UserSession(
            userId = userId,
            userName = prefs.getString("user_name", "") ?: "",
            username = prefs.getString("username", "") ?: "",
            roleKey = prefs.getString("role_key", null),
            clientId = if (clientId == -1L) null else clientId,
            clientName = prefs.getString("client_name", null),
            elementTypeId = if (elementTypeId == -1L) null else elementTypeId,
            elementTypeName = prefs.getString("element_type_name", null),
            token = token
        )
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
