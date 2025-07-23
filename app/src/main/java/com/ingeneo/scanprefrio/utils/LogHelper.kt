package com.ingeneo.scanprefrio.utils

import android.util.Log
import com.ingeneo.scanprefrio.config.AppConfig

/**
 * Clase auxiliar para facilitar la visualización de logs en la aplicación
 */
object LogHelper {
    // Tags principales
    const val API_TAG = AppConfig.LogTags.API_CALL
    const val SYNC_TAG = AppConfig.LogTags.SYNC_WORKER
    const val SCAN_TAG = AppConfig.LogTags.SCAN_QR
    
    /**
     * Imprime un mensaje log muy visible para depuración
     */
    fun debugBanner(tag: String, message: String) {
        val separator = "====================================================="
        Log.e(tag, separator)
        Log.e(tag, "🔍 $message")
        Log.e(tag, separator)
    }
    
    /**
     * Registra información detallada sobre llamadas a la API
     */
    fun logApiCall(endpoint: String, method: String) {
        debugBanner(API_TAG, "LLAMADA API A: $endpoint ($method)")
    }
    
    /**
     * Registra respuestas de la API
     */
    fun logApiResponse(status: Int, body: String?) {
        val statusMsg = if (status in 200..299) "EXITOSA" else "ERROR"
        Log.e(API_TAG, "📡 RESPUESTA API: $status ($statusMsg)")
        body?.let {
            if (it.length > 500) {
                Log.e(API_TAG, "📄 CUERPO: ${it.substring(0, 500)}...")
            } else {
                Log.e(API_TAG, "📄 CUERPO: $it")
            }
        }
    }
    
    /**
     * Registra un error de API
     */
    fun logApiError(endpoint: String, error: Throwable) {
        Log.e(API_TAG, "❌ ERROR EN LLAMADA A $endpoint")
        Log.e(API_TAG, "❌ ${error.javaClass.simpleName}: ${error.message}")
    }
}
