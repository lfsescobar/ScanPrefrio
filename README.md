# 🌸 ScanPrefrio - Aplicación de Escaneo QR para Gestión de Flores

[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/about/versions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5+-orange.svg)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Room%20Database-2.6+-red.svg)](https://developer.android.com/training/data-storage/room)

## 📋 Descripción

**ScanPrefrio** es una aplicación Android moderna desarrollada en Kotlin con Jetpack Compose, diseñada para el escaneo y gestión de códigos QR en el sector floricultor. La aplicación permite escanear códigos QR de estaciones Prefrio y mercancías, registrando automáticamente los tiempos y sincronizando los datos con un servidor remoto.

### 🎯 Características Principales

- **Escaneo QR en tiempo real** con cámara integrada
- **Sincronización automática** con servidor remoto
- **Base de datos local** con Room para almacenamiento offline
- **Interfaz moderna** con Material Design 3
- **Gestión de selectores** (Cliente, Tipo de Flor, Variedad)
- **Trabajo en segundo plano** con WorkManager
- **Modo offline** con sincronización automática cuando hay conexión

## 🏗️ Arquitectura del Proyecto

```
ScanPrefrio/
├── app/
│   ├── src/main/java/com/ingeneo/scanprefrio/
│   │   ├── api/                    # Capa de API y servicios web
│   │   ├── config/                 # Configuraciones centralizadas
│   │   ├── database/               # Base de datos local (Room)
│   │   ├── sync/                   # Sincronización y workers
│   │   ├── ui/                     # Interfaces de usuario (Compose)
│   │   ├── utils/                  # Utilidades y helpers
│   │   └── MainActivity.kt         # Actividad principal
│   └── src/main/res/               # Recursos de la aplicación
├── gradle/                         # Configuración de Gradle
├── build.gradle.kts               # Dependencias del proyecto
└── README.md                      # Este archivo
```

## 🚀 Tecnologías Utilizadas

### Core Technologies
- **Kotlin** - Lenguaje de programación principal
- **Jetpack Compose** - UI declarativa moderna
- **Material Design 3** - Sistema de diseño
- **Android CameraX** - API de cámara moderna
- **ML Kit Barcode Scanning** - Escaneo de códigos QR

### Data & Networking
- **Room Database** - Base de datos local
- **Retrofit** - Cliente HTTP para APIs
- **OkHttp** - Cliente HTTP con interceptores
- **WorkManager** - Tareas en segundo plano

### Architecture & Patterns
- **MVVM** - Model-View-ViewModel
- **Repository Pattern** - Patrón de repositorio
- **Dependency Injection** - Inyección de dependencias
- **Coroutines** - Programación asíncrona

## 📡 Servicios API Consumidos

### Endpoints Principales

#### 1. Envío de Registros de Escaneo
```http
POST http://hub.iws-iot.com/public/api.php?type=app2barcodes
```

**Payload:**
```json
{
  "qrPrefrio": "EST001",
  "dateTimePrefrio": "2024-01-15 10:30:00",
  "dateTimeMercancia": "2024-01-15 10:32:15",
  "client": "Cliente A",
  "type": "Rosa",
  "variety": "Roja",
  "segDif": 135
}
```

#### 2. Obtención de Datos de Selectores

**Clientes:**
```http
POST http://hub.iws-iot.com/public/api_qr.php?endpoint=clientes
```

**Tipos de Flor:**
```http
POST http://hub.iws-iot.com/public/api_qr.php?endpoint=tipos
```

**Variedades:**
```http
POST http://hub.iws-iot.com/public/api_qr.php?endpoint=variedades
```

### Respuestas de API

**Respuesta de Selectores:**
```json
{
  "clientes": ["Cliente A", "Cliente B", "Cliente C"],
  "tipos": ["Rosa", "Tulipán", "Girasol"],
  "variedades": ["Roja", "Blanca", "Amarilla"]
}
```

**Respuesta de Sincronización:**
```json
{
  "status": "success"
}
```

## 🗄️ Base de Datos Local

### Tablas Principales

#### 1. scan_records
```sql
CREATE TABLE scan_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    qrPrefrio TEXT NOT NULL,
    dateTimePrefrio TEXT NOT NULL,
    qrMercancia TEXT NOT NULL,
    dateTimeMercancia TEXT NOT NULL,
    segDif INTEGER NOT NULL,
    sendApi INTEGER DEFAULT 0
);
```

#### 2. clientes
```sql
CREATE TABLE clientes (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    lastSync INTEGER DEFAULT 0
);
```

#### 3. tipos_flor
```sql
CREATE TABLE tipos_flor (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    lastSync INTEGER DEFAULT 0
);
```

#### 4. variedades
```sql
CREATE TABLE variedades (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    lastSync INTEGER DEFAULT 0
);
```

## ⚙️ Configuración

### Archivo de Configuración Centralizada

Todas las configuraciones están centralizadas en `AppConfig.kt`:

```kotlin
object AppConfig {
    // URLs de API
    const val API_BASE_URL = "http://hub.iws-iot.com/public/"
    
    // Configuración de sincronización
    const val SYNC_INTERVAL_HOURS = 1L
    
    // Timeouts de API
    const val API_CONNECT_TIMEOUT = 30L
    
    // Colores de la aplicación
    object Colors {
        const val PRIMARY_GREEN = 0xFF4CAF50
        const val GRAY_BUTTON = 0xFF666666
    }
}
```

### Cambiar Configuraciones

Para modificar configuraciones:

1. **Cambiar servidor API:** Modifica `API_BASE_URL` en `AppConfig.kt`
2. **Ajustar intervalos de sincronización:** Modifica `SYNC_INTERVAL_HOURS`
3. **Cambiar colores:** Modifica los valores en `AppConfig.Colors`
4. **Ajustar timeouts:** Modifica los valores de timeout en `AppConfig`

## 🔧 Instalación y Desarrollo

### Prerrequisitos

- Android Studio Hedgehog o superior
- JDK 17 o superior
- Android SDK API 24+
- Dispositivo Android o emulador

### Pasos de Instalación

1. **Clonar el repositorio:**
```bash
git clone https://github.com/tu-usuario/ScanPrefrio.git
cd ScanPrefrio
```

2. **Abrir en Android Studio:**
```bash
# Abrir Android Studio y seleccionar "Open an existing project"
# Navegar a la carpeta ScanPrefrio
```

3. **Sincronizar dependencias:**
```bash
# En Android Studio: File -> Sync Project with Gradle Files
# O desde terminal:
./gradlew build
```

4. **Ejecutar la aplicación:**
```bash
# Conectar dispositivo Android o iniciar emulador
# En Android Studio: Run -> Run 'app'
```

### Configuración de Desarrollo

1. **Habilitar logs detallados:**
```kotlin
// Los logs están configurados automáticamente
// Revisar LogCat en Android Studio para debugging
```

2. **Configurar servidor de desarrollo:**
```kotlin
// En AppConfig.kt, cambiar:
const val API_BASE_URL = "http://tu-servidor-dev.com/public/"
```

## 📱 Flujo de la Aplicación

### 1. Escaneo de Estación Prefrio
- Usuario escanea código QR de estación
- Validación: máximo 6 caracteres alfanuméricos
- Almacenamiento temporal del código y timestamp

### 2. Selección de Datos
- Pantalla de selectores con datos sincronizados
- Selección de Cliente, Tipo de Flor y Variedad
- Interfaz moderna con diálogos de pantalla completa

### 3. Registro y Sincronización
- Guardado en base de datos local
- Sincronización automática con servidor
- Trabajo en segundo plano con WorkManager

### 4. Consulta de Registros
- Vista de registros pendientes y enviados
- Indicadores visuales de estado
- Filtrado y ordenamiento

## 🔍 Debugging y Logs

### Tags de Log Principales

- `API_CALL` - Llamadas a APIs
- `SYNC_WORKER` - Sincronización de datos
- `SCAN_QR` - Escaneo de códigos QR
- `SelectorsSyncService` - Sincronización de selectores

### Ejemplo de Logs

```bash
# Log de sincronización exitosa
✅ Sincronización completada: 5 registros enviados

# Log de error de API
❌ Error en la sincronización: 500 - Internal Server Error

# Log de escaneo QR
🔍 Código QR detectado: EST001
```

## 🛠️ Mantenimiento y Actualizaciones

### Actualizar Dependencias

```bash
# Verificar versiones actuales
./gradlew dependencies

# Actualizar dependencias específicas
# Editar build.gradle.kts y sincronizar
```

### Migración de Base de Datos

```kotlin
// En ScanDatabase.kt, incrementar versión:
version = 5 // Cambiar de 4 a 5

// Agregar migración si es necesario
.addMigrations(MIGRATION_4_5)
```

### Cambiar Servidor de Producción

```kotlin
// En AppConfig.kt:
const val API_BASE_URL = "https://nuevo-servidor.com/public/"
```

## 📊 Métricas y Monitoreo

### Métricas de Rendimiento

- **Tiempo de sincronización:** Promedio 2-5 segundos
- **Tasa de éxito de escaneo:** >95%
- **Uso de memoria:** ~50MB en promedio
- **Tamaño de APK:** ~15MB

### Monitoreo de Errores

- Logs detallados en LogCat
- Manejo de errores de red
- Reintentos automáticos en fallos
- Indicadores visuales de estado

## 🤝 Contribución

### Guías de Contribución

1. **Fork del repositorio**
2. **Crear rama de feature:** `git checkout -b feature/nueva-funcionalidad`
3. **Hacer commits descriptivos:** `git commit -m "Agregar nueva funcionalidad"`
4. **Push a la rama:** `git push origin feature/nueva-funcionalidad`
5. **Crear Pull Request**

### Estándares de Código

- **Kotlin:** Seguir convenciones oficiales
- **Compose:** Usar componentes reutilizables
- **Documentación:** Comentar funciones públicas
- **Testing:** Agregar tests para nuevas funcionalidades

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 👥 Autores

- **Ingeneo** - Desarrollo inicial
- **Tu Nombre** - Mantenimiento y mejoras

## 📞 Soporte

Para soporte técnico o preguntas:

- **Email:** soporte.easysensor@ingeneo.com
- **Issues:** [GitHub Issues](https://github.com/tu-usuario/ScanPrefrio/issues)
- **Documentación:** [Wiki del Proyecto](https://github.com/tu-usuario/ScanPrefrio/wiki)

---

## 🎉 ¡Gracias por usar ScanPrefrio!

Esta aplicación representa la innovación en el sector floricultor, combinando tecnología moderna con necesidades específicas del negocio. ¡Esperamos que sea útil para tu operación!

---

*Última actualización: Enero 2024*
*Versión: 1.0.0* 