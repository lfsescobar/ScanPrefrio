package com.ingeneo.scanprefrio.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log
import com.ingeneo.scanprefrio.config.AppConfig

object SelectorsRetrofitClient {
    private const val BASE_URL = AppConfig.API_BASE_URL
    private const val TAG = AppConfig.LogTags.SELECTORS_API

    private val loggingInterceptor = HttpLoggingInterceptor().apply { 
        level = HttpLoggingInterceptor.Level.BODY 
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            val requestUrl = request.url.toString()
            
            // Log detallado de la solicitud
            Log.d(TAG, "🚀 ENVIANDO SOLICITUD SELECTORS API")
            Log.d(TAG, "📌 URL: $requestUrl")
            Log.d(TAG, "📋 Método: ${request.method}")
            
            try {
                val startTime = System.currentTimeMillis()
                val response = chain.proceed(request)
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                // Log detallado de la respuesta
                val responseCode = response.code
                Log.d(TAG, "✅ RESPUESTA SELECTORS RECIBIDA (${duration}ms)")
                Log.d(TAG, "📊 Código: $responseCode (${if (response.isSuccessful) "ÉXITO" else "ERROR"})")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR EN SOLICITUD SELECTORS API", e)
                Log.e(TAG, "📌 URL: $requestUrl")
                Log.e(TAG, "❗ Mensaje de error: ${e.message}")
                throw e
            }
        }
        .connectTimeout(AppConfig.API_CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(AppConfig.API_READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.API_WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
} 