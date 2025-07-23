package com.ingeneo.scanprefrio.api

data class ScanRecordApiModel(
    val qrPrefrio: String,
    val dateTimePrefrio: String,
    val dateTimeMercancia: String,
    val client: String,
    val type: String,
    val variety: String,
    val segDif: Long
)

// Actualizando la estructura de la respuesta según el nuevo formato
data class ApiResponse(
    val status: String
)

// Nuevos modelos para los selectores
data class Cliente(
    val id: String,
    val nombre: String
)

data class TipoPlor(
    val id: String,
    val nombre: String
)

data class Variedad(
    val id: String,
    val nombre: String
)

// Respuestas para los nuevos endpoints con formato JSON simple
data class ClientesResponse(
    val clientes: List<String>
)

data class TiposResponse(
    val tipos: List<String>
)

data class VariedadesResponse(
    val variedades: List<String>
)
