package com.ingeneo.scanprefrio.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log
import okhttp3.ResponseBody
import okio.Buffer
import java.nio.charset.Charset
import com.ingeneo.scanprefrio.config.AppConfig

object RetrofitClient {
    private const val BASE_URL = AppConfig.API_BASE_URL
    private const val FULL_API_URL = BASE_URL + AppConfig.API_ENDPOINT_SCAN_RECORDS
    private const val TAG = AppConfig.LogTags.API_CALL

    private val loggingInterceptor = HttpLoggingInterceptor().apply { 
        level = HttpLoggingInterceptor.Level.BODY 
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            val requestUrl = request.url.toString()
            
            // Log detallado de la solicitud
            Log.e(TAG, "🚀 ENVIANDO SOLICITUD API")
            Log.e(TAG, "📌 URL: $requestUrl")
            Log.e(TAG, "💼 URL esperada: $FULL_API_URL")
            Log.e(TAG, "📋 Método: ${request.method}")
            
            // Loguear el cuerpo de la solicitud si existe
            request.body?.let {
                val buffer = Buffer()
                it.writeTo(buffer)
                val charset = Charset.forName("UTF-8")
                val bodyText = buffer.readString(charset)
                Log.e(TAG, "📦 Cuerpo solicitud: $bodyText")
            }
            
            try {
                val startTime = System.currentTimeMillis()
                val response = chain.proceed(request)
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                // Log detallado de la respuesta
                val responseCode = response.code
                Log.e(TAG, "✅ RESPUESTA RECIBIDA (${duration}ms)")
                Log.e(TAG, "📊 Código: $responseCode (${if (response.isSuccessful) "ÉXITO" else "ERROR"})")
                
                // Copiar el cuerpo para no consumirlo
                val responseBody = response.body
                val responseBodyString = responseBody?.string()
                
                // Log del cuerpo de la respuesta
                if (responseBodyString != null) {
                    if (responseBodyString.length > 5000) {
                        Log.e(TAG, "📄 Cuerpo respuesta (truncado): ${responseBodyString.substring(0, 5000)}...")
                    } else {
                        Log.e(TAG, "📄 Cuerpo respuesta: $responseBodyString")
                    }
                    
                    // Reconstruir el cuerpo para evitar "El cuerpo de la respuesta ya fue consumido"
                    val newResponseBody = ResponseBody.create(responseBody.contentType(), responseBodyString)
                    return@addInterceptor response.newBuilder().body(newResponseBody).build()
                }
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR EN SOLICITUD API", e)
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
