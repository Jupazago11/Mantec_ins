package com.example.mantec_ins.data.remote

import com.example.mantec_ins.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager,
    // false solo para los clientes Retrofit que usan los WorkManager
    // periodicos (SyncWorker/SupervisorSyncWorker) — ver
    // RetrofitClient.kt param "background". Un 401 ahi puede disparar
    // con la app en background (pantalla apagada, ni el usuario mirando);
    // forzar un logout global en ese momento saca al usuario de lo que
    // estuviera haciendo sin aviso ni explicacion. Un 401 en una accion
    // en primer plano (login, guardar, boton de sincronizar manual) si
    // debe seguir avisando/forzando re-login de inmediato.
    private val emitExpirationEvent: Boolean = true
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.getToken()

        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401 && !token.isNullOrBlank() && emitExpirationEvent) {
            TokenExpirationEvent.emit()
        }

        return response
    }
}
