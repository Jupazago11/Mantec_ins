package com.example.mantec_ins.data.remote

import android.content.Context
import com.example.mantec_ins.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://mantecsas.com/"
    //private const val BASE_URL = "http://10.0.2.2:8000/"

    private fun buildOkHttpClient(context: Context): OkHttpClient {
        val sessionManager = SessionManager(context)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
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
