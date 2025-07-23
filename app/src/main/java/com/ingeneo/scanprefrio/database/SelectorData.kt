package com.ingeneo.scanprefrio.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "tipos_flor")
data class TipoFlorEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "variedades")
data class VariedadEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val lastSync: Long = System.currentTimeMillis()
) 