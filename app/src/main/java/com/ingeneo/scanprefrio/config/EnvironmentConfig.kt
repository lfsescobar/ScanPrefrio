package com.ingeneo.scanprefrio.config

/**
 * Configuración de entornos para la aplicación ScanPrefrio
 * 
 * Este archivo permite cambiar fácilmente entre diferentes
 * entornos (desarrollo, staging, producción) sin modificar
 * el código principal.
 */
object EnvironmentConfig {
    
    /**
     * Entorno actual de la aplicación
     * Cambiar este valor para cambiar de entorno
     */
    private val CURRENT_ENVIRONMENT = Environment.DEVELOPMENT
    
    /**
     * Enumeración de entornos disponibles
     */
    enum class Environment {
        DEVELOPMENT,
        STAGING,
        PRODUCTION
    }
    
    /**
     * Configuración específica por entorno
     */
    private val environmentConfigs = mapOf(
        Environment.DEVELOPMENT to DevConfig(),
        Environment.STAGING to StagingConfig(),
        Environment.PRODUCTION to ProductionConfig()
    )
    
    /**
     * Obtener la configuración del entorno actual
     */
    fun getCurrentConfig(): BaseConfig {
        return environmentConfigs[CURRENT_ENVIRONMENT] ?: DevConfig()
    }
    
    /**
     * Configuración base para todos los entornos
     */
    abstract class BaseConfig {
        abstract val apiBaseUrl: String
        abstract val apiTimeoutSeconds: Long
        abstract val syncIntervalHours: Long
        abstract val enableDetailedLogs: Boolean
        abstract val enableCrashReporting: Boolean
        abstract val maxRetryAttempts: Int
    }
    
    /**
     * Configuración para entorno de desarrollo
     */
    class DevConfig : BaseConfig() {
        override val apiBaseUrl: String = "http://192.168.1.100:8080/public/"
        override val apiTimeoutSeconds: Long = 60L
        override val syncIntervalHours: Long = 1L
        override val enableDetailedLogs: Boolean = true
        override val enableCrashReporting: Boolean = false
        override val maxRetryAttempts: Int = 3
    }
    
    /**
     * Configuración para entorno de staging
     */
    class StagingConfig : BaseConfig() {
        override val apiBaseUrl: String = "https://staging.hub.iws-iot.com/public/"
        override val apiTimeoutSeconds: Long = 30L
        override val syncIntervalHours: Long = 1L
        override val enableDetailedLogs: Boolean = true
        override val enableCrashReporting: Boolean = true
        override val maxRetryAttempts: Int = 5
    }
    
    /**
     * Configuración para entorno de producción
     */
    class ProductionConfig : BaseConfig() {
        override val apiBaseUrl: String = "https://hub.iws-iot.com/public/"
        override val apiTimeoutSeconds: Long = 30L
        override val syncIntervalHours: Long = 1L
        override val enableDetailedLogs: Boolean = false
        override val enableCrashReporting: Boolean = true
        override val maxRetryAttempts: Int = 3
    }
}

/**
 * Configuración dinámica que se adapta al entorno actual
 */
object DynamicAppConfig {
    
    private val currentConfig = EnvironmentConfig.getCurrentConfig()
    
    // API Configuration
    val API_BASE_URL: String = currentConfig.apiBaseUrl
    val API_TIMEOUT_SECONDS: Long = currentConfig.apiTimeoutSeconds
    
    // Sync Configuration
    val SYNC_INTERVAL_HOURS: Long = currentConfig.syncIntervalHours
    val MAX_RETRY_ATTEMPTS: Int = currentConfig.maxRetryAttempts
    
    // Logging Configuration
    val ENABLE_DETAILED_LOGS: Boolean = currentConfig.enableDetailedLogs
    val ENABLE_CRASH_REPORTING: Boolean = currentConfig.enableCrashReporting
    
    // Feature Flags
    val ENABLE_OFFLINE_MODE: Boolean = true
    val ENABLE_AUTO_SYNC: Boolean = true
    val ENABLE_QR_VALIDATION: Boolean = true
    
    // UI Configuration
    val ENABLE_ANIMATIONS: Boolean = true
    val ENABLE_HAPTIC_FEEDBACK: Boolean = true
    
    // Debug Configuration
    val ENABLE_DEBUG_MENU: Boolean = currentConfig.enableDetailedLogs
    val ENABLE_PERFORMANCE_MONITORING: Boolean = currentConfig.enableDetailedLogs
}

/**
 * Utilidades para cambiar configuración en tiempo de ejecución
 * (Solo para desarrollo y debugging)
 */
object RuntimeConfig {
    
    private var customApiUrl: String? = null
    private var customSyncInterval: Long? = null
    private var customLogLevel: Boolean? = null
    
    /**
     * Establecer URL de API personalizada
     */
    fun setCustomApiUrl(url: String) {
        customApiUrl = url
    }
    
    /**
     * Obtener URL de API (personalizada o por defecto)
     */
    fun getApiUrl(): String {
        return customApiUrl ?: DynamicAppConfig.API_BASE_URL
    }
    
    /**
     * Establecer intervalo de sincronización personalizado
     */
    fun setCustomSyncInterval(hours: Long) {
        customSyncInterval = hours
    }
    
    /**
     * Obtener intervalo de sincronización
     */
    fun getSyncInterval(): Long {
        return customSyncInterval ?: DynamicAppConfig.SYNC_INTERVAL_HOURS
    }
    
    /**
     * Establecer nivel de logging personalizado
     */
    fun setCustomLogLevel(enabled: Boolean) {
        customLogLevel = enabled
    }
    
    /**
     * Obtener nivel de logging
     */
    fun getLogLevel(): Boolean {
        return customLogLevel ?: DynamicAppConfig.ENABLE_DETAILED_LOGS
    }
    
    /**
     * Resetear configuraciones personalizadas
     */
    fun resetCustomConfig() {
        customApiUrl = null
        customSyncInterval = null
        customLogLevel = null
    }
} 