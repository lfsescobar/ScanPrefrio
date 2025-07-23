package com.ingeneo.scanprefrio.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val qrPrefrio: String,
    val dateTimePrefrio: String,
    val qrMercancia: String,
    val dateTimeMercancia: String,
    val segDif: Long,
    val sendApi: Int // 0 = no sincronizado, 1 = sincronizado
)
