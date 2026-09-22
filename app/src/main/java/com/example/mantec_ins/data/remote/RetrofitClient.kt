package com.example.mantec_ins.data.remote

import android.content.Context
import com.example.mantec_ins.BuildConfig
import com.example.mantec_ins.data.local.SessionManager
import com.example.mantec_ins.data.remote.personal.PersonalApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // OJO antes de commitear: dejar SOLO la linea de produccion activa.
    // Ya paso una vez (commit 6cb0d5c) que esto quedo apuntando al
    // emulador en un commit real.
    private val BASE_URL = "https://mantecsas.com/" // produccion
    // private val BASE_URL = "http://10.0.2.2:8000/" // local (emulador -> localhost:8000 del host)

    // El backend acepta evidencia de hasta 1 GB (ver v1.7.6). Los inspectores
    // suben esa evidencia desde el campo, muchas veces con datos móviles y
    // señal débil, así que el timeout debe tolerar una subida lenta en vez de
    // cortarla prematuramente. max_execution_time=300s en el backend no acota
    // esto: ese contador es de ejecución del script PHP, no del tiempo que
    // tarda el archivo en transmitirse hasta el servidor.
    private const val UPLOAD_TIMEOUT_SECONDS = 900L // 15 minutos
    private const val CONNECT_TIMEOUT_SECONDS = 20L

    private fun buildOkHttpClient(context: Context, emitExpirationEvent: Boolean): OkHttpClient {
        val sessionManager = SessionManager(context)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager, emitExpirationEvent))
            .addInterceptor(logging)
            .build()
    }

    private fun buildRetrofit(context: Context, emitExpirationEvent: Boolean = true): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttpClient(context, emitExpirationEvent))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthApiService(context: Context): AuthApiService {
        return buildRetrofit(context).create(AuthApiService::class.java)
    }

    // background=true: para SyncWorker (WorkManager periodico) — ver
    // AuthInterceptor.emitExpirationEvent. Un 401 durante un sync
    // silencioso en segundo plano no debe forzar un logout global sin
    // que el usuario este mirando la app.
    fun createSyncApiService(context: Context, background: Boolean = false): SyncApiService {
        return buildRetrofit(context, emitExpirationEvent = !background).create(SyncApiService::class.java)
    }

    fun createMeasurementApiService(context: Context, background: Boolean = false): MeasurementApiService {
        return buildRetrofit(context, emitExpirationEvent = !background).create(MeasurementApiService::class.java)
    }

    // Mismo AuthInterceptor (agrega "Authorization: Bearer <token>" desde
    // SessionManager) que el resto — un dispositivo esta logueado como
    // Inspector O Supervisor, nunca ambos a la vez, asi que un solo token
    // guardado alcanza (ver SessionManager/UserSession.roleKey).
    // background=true: para SupervisorSyncWorker, mismo criterio que
    // createSyncApiService.
    fun createPersonalApiService(context: Context, background: Boolean = false): PersonalApiService {
        return buildRetrofit(context, emitExpirationEvent = !background).create(PersonalApiService::class.java)
    }
}
