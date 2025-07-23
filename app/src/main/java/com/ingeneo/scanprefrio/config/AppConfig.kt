package com.ingeneo.scanprefrio.config

/**
 * Configuración centralizada de la aplicación ScanPrefrio
 * 
 * Este archivo contiene todas las constantes y configuraciones
 * que anteriormente estaban "quemadas" en el código.
 * 
 * Para cambiar configuraciones, modifica los valores aquí.
 */
object AppConfig {
    
    // ==================== API CONFIGURATION ====================
    
    /**
     * URL base del servidor API
     * Cambiar esta URL para apuntar a un servidor diferente
     */
    const val API_BASE_URL = "http://hub.iws-iot.com/public/"
    
    /**
     * Endpoint para enviar registros de escaneo
     */
    const val API_ENDPOINT_SCAN_RECORDS = "api.php?type=app2barcodes"
    
    /**
     * Endpoint para obtener datos de selectores
     */
    const val API_ENDPOINT_SELECTORS = "api_qr.php"
    
    // ==================== DATABASE CONFIGURATION ====================
    
    /**
     * Nombre de la base de datos local
     */
    const val DATABASE_NAME = "scan_database"
    
    /**
     * Versión actual de la base de datos
     * Incrementar cuando se hagan cambios en el esquema
     */
    const val DATABASE_VERSION = 4
    
    // ==================== SYNC CONFIGURATION ====================
    
    /**
     * Intervalo de sincronización automática (en horas)
     */
    const val SYNC_INTERVAL_HOURS = 1L
    
    /**
     * Timeouts para llamadas API (en segundos)
     */
    const val API_CONNECT_TIMEOUT = 30L
    const val API_READ_TIMEOUT = 30L
    const val API_WRITE_TIMEOUT = 30L
    
    // ==================== UI CONFIGURATION ====================
    
    /**
     * Colores principales de la aplicación
     */
    object Colors {
        const val PRIMARY_GREEN = 0xFF4CAF50
        const val GRAY_BUTTON = 0xFF666666
        const val DARK_BACKGROUND = 0xFF333333
        const val LIGHT_GRAY = 0xFFF5F5F5
    }
    
    /**
     * Configuración de validación de códigos QR
     */
    object QRValidation {
        const val MAX_STATION_CODE_LENGTH = 6
        const val INITIAL_SCAN_DELAY_MS = 0L
        const val MERCHANDISE_SCAN_DELAY_MS = 1500L
    }
    
    // ==================== WORKER NAMES ====================
    
    /**
     * Nombres de los workers para sincronización
     */
    object WorkerNames {
        const val SYNC_WORK = "sync_work"
        const val IMMEDIATE_SYNC_WORK = "immediate_sync_work"
    }
    
    // ==================== LOG TAGS ====================
    
    /**
     * Tags para logging y debugging
     */
    object LogTags {
        const val API_CALL = "API_CALL"
        const val SYNC_WORKER = "SYNC_WORKER"
        const val SCAN_QR = "SCAN_QR"
        const val SELECTORS_API = "SELECTORS_API_CALL"
        const val APP_STARTUP = "APP_STARTUP"
        const val SELECTORS_SYNC = "SelectorsSyncService"
    }
    
    // ==================== NETWORK SECURITY ====================
    
    /**
     * Dominios permitidos para tráfico HTTP sin cifrar
     */
    object AllowedDomains {
        const val HUB_DOMAIN = "hub.iws-iot.com"
        const val SERVICES_DOMAIN = "services.iws-iot.com"
    }
    
    // ==================== CAMERA CONFIGURATION ====================
    
    /**
     * Configuración de la cámara para escaneo QR
     */
    object CameraConfig {
        const val TARGET_WIDTH = 1280
        const val TARGET_HEIGHT = 720
    }
    
    // ==================== ENDPOINTS ====================
    
    /**
     * Endpoints específicos para los selectores
     */
    object SelectorEndpoints {
        const val CLIENTES = "clientes"
        const val TIPOS = "tipos"
        const val VARIEDADES = "variedades"
    }
} 