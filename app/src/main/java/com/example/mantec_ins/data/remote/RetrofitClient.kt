package com.example.mantec_ins.data.remote

import android.content.Context
import com.example.mantec_ins.BuildConfig
import com.example.mantec_ins.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://mantecsas.com/"
    //private const val BASE_URL = "http://10.0.2.2:8000/"

    // El backend acepta evidencia de hasta 1 GB (ver v1.7.6). Los inspectores
    // suben esa evidencia desde el campo, muchas veces con datos móviles y
    // señal débil, así que el timeout debe tolerar una subida lenta en vez de
    // cortarla prematuramente. max_execution_time=300s en el backend no acota
    // esto: ese contador es de ejecución del script PHP, no del tiempo que
    // tarda el archivo en transmitirse hasta el servidor.
    private const val UPLOAD_TIMEOUT_SECONDS = 900L // 15 minutos
    private const val CONNECT_TIMEOUT_SECONDS = 20L

    private fun buildOkHttpClient(context: Context): OkHttpClient {
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
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()
    }

    private fun buildRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthApiService(context: Context): AuthApiService {
        return buildRetrofit(context).create(AuthApiService::class.java)
    }

    fun createSyncApiService(context: Context): SyncApiService {
        return buildRetrofit(context).create(SyncApiService::class.java)
    }

    fun createMeasurementApiService(context: Context): MeasurementApiService {
        return buildRetrofit(context).create(MeasurementApiService::class.java)
    }
}
