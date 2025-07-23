package com.ingeneo.scanprefrio.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ingeneo.scanprefrio.api.ScanRecordApiModel

class ScanRepository(private val scanDao: ScanDao) {

    // Obtener todos los registros
    fun getAllRecords(): LiveData<List<ScanRecord>> {
        return scanDao.getAllRecords()
    }

    // Insertar un nuevo registro
    suspend fun insertScanRecord(scanRecord: ScanRecord): Long {
        return scanDao.insertScanRecord(scanRecord)
    }

    // Obtener registros no sincronizados (sendApi = 0)
    suspend fun getUnsyncedRecords(): List<ScanRecord> {
        return scanDao.getUnsyncedRecords()
    }
    
    // Convertir ScanRecord a ScanRecordApiModel con el nuevo formato
    fun convertToApiModel(record: ScanRecord): ScanRecordApiModel {
        // Extraer client, type, variety del campo qrMercancia
        val parts = record.qrMercancia.split(" - ")
        val client = if (parts.size >= 1) parts[0] else ""
        val type = if (parts.size >= 2) parts[1] else ""
        val variety = if (parts.size >= 3) parts[2] else ""
        
        return ScanRecordApiModel(
            qrPrefrio = record.qrPrefrio,
            dateTimePrefrio = record.dateTimePrefrio,
            dateTimeMercancia = record.dateTimeMercancia,
            client = client,
            type = type,
            variety = variety,
            segDif = record.segDif
        )
    }

    // Actualizar estado de sincronización
    suspend fun updateSyncStatus(id: Int, syncStatus: Int) {
        scanDao.updateSyncStatus(id, syncStatus)
    }

    // Obtener registros sincronizados (sendApi = 1)
    suspend fun getSyncedRecords(): List<ScanRecord> {
        return scanDao.getSyncedRecords()
    }

    // Obtener registros no enviados y los últimos 10 enviados
    suspend fun getPendingAndRecentSyncedRecords(): List<ScanRecord> {
        return scanDao.getPendingAndRecentSyncedRecords()
    }
    
    // Operaciones para Clientes
    suspend fun getAllClientes(): List<ClienteEntity> {
        return withContext(Dispatchers.IO) {
            scanDao.getAllClientes()
        }
    }
    
    suspend fun insertClientes(clientes: List<ClienteEntity>) {
        withContext(Dispatchers.IO) {
            scanDao.insertClientes(clientes)
        }
    }
    
    suspend fun deleteAllClientes() {
        withContext(Dispatchers.IO) {
            scanDao.deleteAllClientes()
        }
    }
    
    // Operaciones para Tipos de Flor
    suspend fun getAllTiposFlor(): List<TipoFlorEntity> {
        return withContext(Dispatchers.IO) {
            scanDao.getAllTiposFlor()
        }
    }
    
    suspend fun insertTiposFlor(tipos: List<TipoFlorEntity>) {
        withContext(Dispatchers.IO) {
            scanDao.insertTiposFlor(tipos)
        }
    }
    
    suspend fun deleteAllTiposFlor() {
        withContext(Dispatchers.IO) {
            scanDao.deleteAllTiposFlor()
        }
    }
    
    // Operaciones para Variedades
    suspend fun getAllVariedades(): List<VariedadEntity> {
        return withContext(Dispatchers.IO) {
            scanDao.getAllVariedades()
        }
    }
    
    suspend fun insertVariedades(variedades: List<VariedadEntity>) {
        withContext(Dispatchers.IO) {
            scanDao.insertVariedades(variedades)
        }
    }
    
    suspend fun deleteAllVariedades() {
        withContext(Dispatchers.IO) {
            scanDao.deleteAllVariedades()
        }
    }
}
