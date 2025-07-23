package com.ingeneo.scanprefrio.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScanDao {
    // Obtener todos los registros
    @Query("SELECT * FROM scan_records ORDER BY id DESC")
    fun getAllRecords(): LiveData<List<ScanRecord>>

    // Insertar un nuevo registro
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(scanRecord: ScanRecord): Long

    // Obtener registros no sincronizados
    @Query("SELECT * FROM scan_records WHERE sendApi = 0")
    suspend fun getUnsyncedRecords(): List<ScanRecord>

    // Actualizar estado de sincronización
    @Query("UPDATE scan_records SET sendApi = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, syncStatus: Int)

    // Obtener registros sincronizados
    @Query("SELECT * FROM scan_records WHERE sendApi = 1 ORDER BY id DESC LIMIT 50")
    suspend fun getSyncedRecords(): List<ScanRecord>

    // Obtener registros no enviados y los últimos 10 enviados
    @Query("SELECT * FROM scan_records WHERE sendApi = 0 UNION ALL SELECT * FROM scan_records WHERE sendApi = 1 ORDER BY id DESC LIMIT 10")
    suspend fun getPendingAndRecentSyncedRecords(): List<ScanRecord>
    
    // Operaciones para Clientes
    @Query("SELECT * FROM clientes ORDER BY nombre")
    suspend fun getAllClientes(): List<ClienteEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientes(clientes: List<ClienteEntity>)
    
    @Query("DELETE FROM clientes")
    suspend fun deleteAllClientes()
    
    // Operaciones para Tipos de Flor
    @Query("SELECT * FROM tipos_flor ORDER BY nombre")
    suspend fun getAllTiposFlor(): List<TipoFlorEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiposFlor(tipos: List<TipoFlorEntity>)
    
    @Query("DELETE FROM tipos_flor")
    suspend fun deleteAllTiposFlor()
    
    // Operaciones para Variedades
    @Query("SELECT * FROM variedades ORDER BY nombre")
    suspend fun getAllVariedades(): List<VariedadEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariedades(variedades: List<VariedadEntity>)
    
    @Query("DELETE FROM variedades")
    suspend fun deleteAllVariedades()
}
