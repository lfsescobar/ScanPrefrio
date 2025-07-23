package com.ingeneo.scanprefrio.sync

import android.content.Context
import android.util.Log
import com.ingeneo.scanprefrio.api.SelectorsRetrofitClient
import com.ingeneo.scanprefrio.database.ScanDatabase
import com.ingeneo.scanprefrio.database.ScanRepository
import com.ingeneo.scanprefrio.database.ClienteEntity
import com.ingeneo.scanprefrio.database.TipoFlorEntity
import com.ingeneo.scanprefrio.database.VariedadEntity
import com.ingeneo.scanprefrio.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SelectorsSyncService {
    private const val TAG = AppConfig.LogTags.SELECTORS_SYNC
    
    suspend fun syncSelectorsData(context: Context): Boolean {
        return try {
            Log.d(TAG, "🔄 Iniciando sincronización de datos de selectores...")
            
            val database = ScanDatabase.getDatabase(context)
            val repository = ScanRepository(database.scanDao())
            val apiService = SelectorsRetrofitClient.apiService
            
            // Log de las URLs que se van a probar
            Log.d(TAG, "🌐 URLs a probar (POST):")
            Log.d(TAG, "   - Clientes: POST http://hub.iws-iot.com/public/api_qr.php?endpoint=clientes")
            Log.d(TAG, "   - Tipos: POST http://hub.iws-iot.com/public/api_qr.php?endpoint=tipos")
            Log.d(TAG, "   - Variedades: POST http://hub.iws-iot.com/public/api_qr.php?endpoint=variedades")
            
            var successCount = 0
            var totalCount = 0
            
            // Sincronizar Clientes
            try {
                Log.d(TAG, "📋 Sincronizando clientes...")
                val clientesResponse = apiService.getClientes()
                Log.d(TAG, "📊 Respuesta clientes - Código: ${clientesResponse.code()}, Exitoso: ${clientesResponse.isSuccessful}")
                
                if (clientesResponse.isSuccessful) {
                    val responseBody = clientesResponse.body()
                    Log.d(TAG, "📄 Cuerpo de respuesta clientes: $responseBody")
                    
                    val clientes = responseBody?.clientes ?: emptyList()
                    Log.d(TAG, "📋 Lista de clientes obtenida: ${clientes.size} elementos")
                    
                    if (clientes.isNotEmpty()) {
                        val clientesEntities = clientes.mapIndexed { index, nombre ->
                            ClienteEntity(
                                id = (index + 1).toString(), // Generar ID basado en el índice
                                nombre = nombre
                            )
                        }
                        
                        repository.deleteAllClientes()
                        repository.insertClientes(clientesEntities)
                        
                        Log.d(TAG, "✅ Clientes sincronizados: ${clientesEntities.size}")
                        successCount++
                    } else {
                        Log.w(TAG, "⚠️ Lista de clientes vacía")
                    }
                } else {
                    val errorBody = clientesResponse.errorBody()?.string()
                    Log.e(TAG, "❌ Error al sincronizar clientes: ${clientesResponse.code()}")
                    Log.e(TAG, "❌ Cuerpo de error: $errorBody")
                }
                totalCount++
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al sincronizar clientes", e)
                Log.e(TAG, "❌ Tipo de error: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Mensaje de error: ${e.message}")
            }
            
            // Sincronizar Tipos de Flor
            try {
                Log.d(TAG, "📋 Sincronizando tipos de flor...")
                val tiposResponse = apiService.getTiposPlor()
                Log.d(TAG, "📊 Respuesta tipos - Código: ${tiposResponse.code()}, Exitoso: ${tiposResponse.isSuccessful}")
                
                if (tiposResponse.isSuccessful) {
                    val responseBody = tiposResponse.body()
                    Log.d(TAG, "📄 Cuerpo de respuesta tipos: $responseBody")
                    
                    val tipos = responseBody?.tipos ?: emptyList()
                    Log.d(TAG, "📋 Lista de tipos obtenida: ${tipos.size} elementos")
                    
                    if (tipos.isNotEmpty()) {
                        val tiposEntities = tipos.mapIndexed { index, nombre ->
                            TipoFlorEntity(
                                id = (index + 1).toString(), // Generar ID basado en el índice
                                nombre = nombre
                            )
                        }
                        
                        repository.deleteAllTiposFlor()
                        repository.insertTiposFlor(tiposEntities)
                        
                        Log.d(TAG, "✅ Tipos de flor sincronizados: ${tiposEntities.size}")
                        successCount++
                    } else {
                        Log.w(TAG, "⚠️ Lista de tipos vacía")
                    }
                } else {
                    val errorBody = tiposResponse.errorBody()?.string()
                    Log.e(TAG, "❌ Error al sincronizar tipos de flor: ${tiposResponse.code()}")
                    Log.e(TAG, "❌ Cuerpo de error: $errorBody")
                }
                totalCount++
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al sincronizar tipos de flor", e)
                Log.e(TAG, "❌ Tipo de error: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Mensaje de error: ${e.message}")
            }
            
            // Sincronizar Variedades
            try {
                Log.d(TAG, "📋 Sincronizando variedades...")
                val variedadesResponse = apiService.getVariedades()
                Log.d(TAG, "📊 Respuesta variedades - Código: ${variedadesResponse.code()}, Exitoso: ${variedadesResponse.isSuccessful}")
                
                if (variedadesResponse.isSuccessful) {
                    val responseBody = variedadesResponse.body()
                    Log.d(TAG, "📄 Cuerpo de respuesta variedades: $responseBody")
                    
                    val variedades = responseBody?.variedades ?: emptyList()
                    Log.d(TAG, "📋 Lista de variedades obtenida: ${variedades.size} elementos")
                    
                    if (variedades.isNotEmpty()) {
                        val variedadesEntities = variedades.mapIndexed { index, nombre ->
                            VariedadEntity(
                                id = (index + 1).toString(), // Generar ID basado en el índice
                                nombre = nombre
                            )
                        }
                        
                        repository.deleteAllVariedades()
                        repository.insertVariedades(variedadesEntities)
                        
                        Log.d(TAG, "✅ Variedades sincronizadas: ${variedadesEntities.size}")
                        successCount++
                    } else {
                        Log.w(TAG, "⚠️ Lista de variedades vacía")
                    }
                } else {
                    val errorBody = variedadesResponse.errorBody()?.string()
                    Log.e(TAG, "❌ Error al sincronizar variedades: ${variedadesResponse.code()}")
                    Log.e(TAG, "❌ Cuerpo de error: $errorBody")
                }
                totalCount++
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al sincronizar variedades", e)
                Log.e(TAG, "❌ Tipo de error: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Mensaje de error: ${e.message}")
            }
            
            val success = successCount > 0
            Log.d(TAG, "🏁 Sincronización completada: $successCount/$totalCount exitosas")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error general en sincronización de selectores", e)
            false
        }
    }
    
    suspend fun getLocalClientes(context: Context): List<ClienteEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val database = ScanDatabase.getDatabase(context)
                val repository = ScanRepository(database.scanDao())
                repository.getAllClientes()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al obtener clientes locales", e)
                emptyList()
            }
        }
    }
    
    suspend fun getLocalTiposFlor(context: Context): List<TipoFlorEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val database = ScanDatabase.getDatabase(context)
                val repository = ScanRepository(database.scanDao())
                repository.getAllTiposFlor()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al obtener tipos de flor locales", e)
                emptyList()
            }
        }
    }
    
    suspend fun getLocalVariedades(context: Context): List<VariedadEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val database = ScanDatabase.getDatabase(context)
                val repository = ScanRepository(database.scanDao())
                repository.getAllVariedades()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al obtener variedades locales", e)
                emptyList()
            }
        }
    }
} 