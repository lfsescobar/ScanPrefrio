package com.ingeneo.scanprefrio.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    // Asegurarnos que la URL siempre use app2barcodes sin posibilidad de sobrescribirla
    @POST("api.php?type=app2barcodes")
    suspend fun sendScanRecords(
        @Body records: List<ScanRecordApiModel>
    ): Response<ApiResponse>
    
    // Nuevos endpoints para los selectores
    @POST("api_qr.php")
    suspend fun getClientes(
        @Query("endpoint") endpoint: String = "clientes"
    ): Response<ClientesResponse>
    
    @POST("api_qr.php")
    suspend fun getTiposPlor(
        @Query("endpoint") endpoint: String = "tipos"
    ): Response<TiposResponse>
    
    @POST("api_qr.php")
    suspend fun getVariedades(
        @Query("endpoint") endpoint: String = "variedades"
    ): Response<VariedadesResponse>
}
