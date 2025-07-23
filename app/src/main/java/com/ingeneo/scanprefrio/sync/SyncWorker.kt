package com.ingeneo.scanprefrio.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ingeneo.scanprefrio.api.ApiService
import com.ingeneo.scanprefrio.api.RetrofitClient
import com.ingeneo.scanprefrio.api.ScanRecordApiModel
import com.ingeneo.scanprefrio.database.ScanDatabase
import com.ingeneo.scanprefrio.database.ScanRepository
import com.ingeneo.scanprefrio.utils.LogHelper
import com.ingeneo.scanprefrio.config.AppConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val SYNC_WORK_NAME = AppConfig.WorkerNames.SYNC_WORK
        private const val IMMEDIATE_SYNC_WORK_NAME = AppConfig.WorkerNames.IMMEDIATE_SYNC_WORK
        
        fun scheduleSync(context: Context) {
            LogHelper.debugBanner(LogHelper.SYNC_TAG, "PROGRAMANDO SINCRONIZACIÓN PERIÓDICA")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                AppConfig.SYNC_INTERVAL_HOURS, TimeUnit.HOURS,  // Sincronizar cada hora
                15, TimeUnit.MINUTES    // Flexibilidad de 15 minutos
            )
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            Log.e(LogHelper.SYNC_TAG, "✅ Sincronización programada cada 15 minutos")
            
            // También ejecutamos una sincronización inmediata
            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueue(oneTimeRequest)
            Log.e(LogHelper.SYNC_TAG, "🚀 Sincronización inmediata solicitada")
        }

        // Nuevo método para solicitar sincronización inmediata tras escanear el segundo QR
        fun syncNow(context: Context) {
            LogHelper.debugBanner(LogHelper.SYNC_TAG, "INICIANDO SINCRONIZACIÓN INMEDIATA TRAS ESCANEO")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            
            Log.e(LogHelper.SYNC_TAG, "🚀 Sincronización inmediata solicitada tras escaneo de mercancía")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LogHelper.debugBanner(LogHelper.SYNC_TAG, "INICIANDO SINCRONIZACIÓN DE DATOS")
            
            // Obtener registros no sincronizados
            val database = ScanDatabase.getDatabase(applicationContext)
            val repository = ScanRepository(database.scanDao())
            val pendingRecords = repository.getUnsyncedRecords() // Método correcto para obtener registros pendientes
            
            // Si no hay registros pendientes, terminar
            if (pendingRecords.isEmpty()) {
                Log.e(LogHelper.SYNC_TAG, "✓ No hay registros pendientes para sincronizar")
                return@withContext Result.success()
            }
            
            Log.e(LogHelper.SYNC_TAG, "📤 Sincronizando ${pendingRecords.size} registros...")
            
            // Registramos los registros que vamos a enviar para depuración
            pendingRecords.forEachIndexed { index, record ->
                Log.e(
                    LogHelper.SYNC_TAG,
                    "Registro ${index + 1}: Prefrio=${record.qrPrefrio}, Mercancia=${record.qrMercancia}, Dif=${record.segDif}s"
                )
            }
            
            // Convertir a modelo de API con el nuevo formato
            val apiModels = pendingRecords.map { record ->
                repository.convertToApiModel(record)
            }
            
            // Actualizado a nueva URL
            LogHelper.logApiCall("api.php?type=app2barcodes", "POST")
            
            try {
                // Ejecutamos la llamada API con mejor manejo de errores
                val apiService = RetrofitClient.apiService
                val response = apiService.sendScanRecords(records = apiModels)
                
                if (response.isSuccessful) {
                    // Marcar registros como sincronizados
                    pendingRecords.forEach { record ->
                        repository.updateSyncStatus(record.id, 1) // Marcar como sincronizado (1)
                    }
                    
                    val body = response.body()
                    LogHelper.logApiResponse(response.code(), body.toString())
                    Log.e(LogHelper.SYNC_TAG, "✅ Sincronización completada: ${pendingRecords.size} registros enviados")
                    
                    Result.success()
                } else {
                    val errorMsg = "Error en la sincronización: ${response.code()} - ${response.message()}"
                    val errorBody = response.errorBody()?.string() ?: "Sin cuerpo de error"
                    LogHelper.logApiResponse(response.code(), errorBody)
                    Log.e(LogHelper.SYNC_TAG, "❌ $errorMsg")
                    Log.e(LogHelper.SYNC_TAG, "❌ Cuerpo de error: $errorBody")
                    
                    Result.retry()
                }
            } catch (e: Exception) {
                // Actualizado a nueva URL
                LogHelper.logApiError("api.php?type=app2barcodes", e)
                Log.e(LogHelper.SYNC_TAG, "❌ Error en la llamada API: ${e.message}")
                e.printStackTrace()
                
                Result.retry()
            }
        } catch (e: Exception) {
            // Actualizado a nueva URL
            LogHelper.logApiError("api.php?type=app2barcodes", e)
            Log.e(LogHelper.SYNC_TAG, "❌ Error general en sincronización: ${e.message}")
            e.printStackTrace()
            
            Result.retry()
        }
    }
}
