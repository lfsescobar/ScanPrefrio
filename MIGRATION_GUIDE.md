# 🔄 Guía de Migración - ScanPrefrio

## 📋 Introducción

Esta guía te ayudará a migrar y actualizar la aplicación ScanPrefrio de manera segura, incluyendo cambios de configuración, actualizaciones de dependencias y modificaciones de base de datos.

## 🔧 Cambios de Configuración

### 1. Cambiar Servidor API

#### Opción A: Usando AppConfig.kt (Recomendado)
```kotlin
// En app/src/main/java/com/ingeneo/scanprefrio/config/AppConfig.kt
object AppConfig {
    // Cambiar esta línea:
    const val API_BASE_URL = "https://nuevo-servidor.com/public/"
}
```

#### Opción B: Usando EnvironmentConfig.kt
```kotlin
// En app/src/main/java/com/ingeneo/scanprefrio/config/EnvironmentConfig.kt
class ProductionConfig : BaseConfig() {
    override val apiBaseUrl: String = "https://nuevo-servidor.com/public/"
}
```

### 2. Cambiar Intervalo de Sincronización

```kotlin
// En AppConfig.kt
const val SYNC_INTERVAL_HOURS = 2L  // Cambiar de 1 a 2 horas

// O en EnvironmentConfig.kt
class ProductionConfig : BaseConfig() {
    override val syncIntervalHours: Long = 2L
}
```

### 3. Cambiar Timeouts de API

```kotlin
// En AppConfig.kt
const val API_CONNECT_TIMEOUT = 45L  // Cambiar de 30 a 45 segundos
const val API_READ_TIMEOUT = 45L
const val API_WRITE_TIMEOUT = 45L
```

### 4. Cambiar Colores de la Aplicación

```kotlin
// En AppConfig.kt
object Colors {
    const val PRIMARY_GREEN = 0xFF2E7D32  // Verde más oscuro
    const val GRAY_BUTTON = 0xFF757575    // Gris más claro
    const val DARK_BACKGROUND = 0xFF212121 // Fondo más oscuro
}
```

## 📦 Actualización de Dependencias

### 1. Verificar Versiones Actuales

```bash
# Ver todas las dependencias
./gradlew dependencies

# Ver dependencias específicas
./gradlew dependencies --configuration implementation
```

### 2. Actualizar Dependencias de Compose

```kotlin
// En app/build.gradle.kts
android {
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"  // Actualizar de 1.5.1
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
}
```

### 3. Actualizar Dependencias de Camera

```kotlin
dependencies {
    // Actualizar CameraX
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
}
```

### 4. Actualizar Dependencias de Base de Datos

```kotlin
dependencies {
    // Actualizar Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}
```

### 5. Actualizar Dependencias de Networking

```kotlin
dependencies {
    // Actualizar Retrofit y OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

## 🗄️ Migración de Base de Datos

### 1. Incrementar Versión de Base de Datos

```kotlin
// En app/src/main/java/com/ingeneo/scanprefrio/database/ScanDatabase.kt
@Database(
    entities = [
        ScanRecord::class,
        ClienteEntity::class,
        TipoFlorEntity::class,
        VariedadEntity::class
    ], 
    version = 5,  // Cambiar de 4 a 5
    exportSchema = false
)
```

### 2. Agregar Nueva Entidad

```kotlin
// 1. Crear nueva entidad
@Entity(tableName = "configuraciones")
data class ConfiguracionEntity(
    @PrimaryKey val id: String,
    val valor: String,
    val lastUpdate: Long = System.currentTimeMillis()
)

// 2. Agregar a la base de datos
@Database(
    entities = [
        ScanRecord::class,
        ClienteEntity::class,
        TipoFlorEntity::class,
        VariedadEntity::class,
        ConfiguracionEntity::class  // Agregar nueva entidad
    ], 
    version = 5,
    exportSchema = false
)
```

### 3. Crear Migración

```kotlin
// En ScanDatabase.kt
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Crear nueva tabla
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS configuraciones (
                id TEXT PRIMARY KEY NOT NULL,
                valor TEXT NOT NULL,
                lastUpdate INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // Insertar datos iniciales
        database.execSQL("""
            INSERT INTO configuraciones (id, valor, lastUpdate) 
            VALUES ('sync_interval', '3600', ${System.currentTimeMillis()})
        """)
    }
}

// Agregar migración al builder
Room.databaseBuilder(
    context.applicationContext,
    ScanDatabase::class.java,
    "scan_database"
)
.fallbackToDestructiveMigration()
.addMigrations(MIGRATION_4_5)  // Agregar migración
.build()
```

### 4. Agregar Operaciones DAO

```kotlin
// En ScanDao.kt
@Dao
interface ScanDao {
    // ... operaciones existentes ...
    
    // Operaciones para Configuraciones
    @Query("SELECT * FROM configuraciones WHERE id = :configId")
    suspend fun getConfiguracion(configId: String): ConfiguracionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguracion(configuracion: ConfiguracionEntity)
    
    @Query("UPDATE configuraciones SET valor = :valor, lastUpdate = :timestamp WHERE id = :configId")
    suspend fun updateConfiguracion(configId: String, valor: String, timestamp: Long)
}
```

## 🔄 Migración de Código

### 1. Actualizar Imports

```kotlin
// Antes
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text

// Después
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
```

### 2. Actualizar APIs de Compose

```kotlin
// Antes
@Composable
fun MyComponent() {
    Surface(color = MaterialTheme.colors.primary) {
        Text("Hello")
    }
}

// Después
@Composable
fun MyComponent() {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Text("Hello")
    }
}
```

### 3. Actualizar APIs de Camera

```kotlin
// Antes
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(1280, 720))
    .build()

// Después
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(1280, 720))
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
```

## 🧪 Testing de Migración

### 1. Verificar Compilación

```bash
# Limpiar y recompilar
./gradlew clean
./gradlew build

# Verificar que no hay errores de compilación
./gradlew assembleDebug
```

### 2. Testing de Base de Datos

```kotlin
@Test
fun testDatabaseMigration() {
    // Crear base de datos con versión anterior
    val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        ScanDatabase::class.java
    ).addMigrations(MIGRATION_4_5).build()
    
    // Verificar que la migración funciona
    val configDao = db.scanDao()
    val config = configDao.getConfiguracion("sync_interval")
    assertNotNull(config)
    assertEquals("3600", config.valor)
}
```

### 3. Testing de API

```kotlin
@Test
fun testApiConfiguration() {
    val apiUrl = AppConfig.API_BASE_URL
    assertTrue(apiUrl.isNotEmpty())
    assertTrue(apiUrl.startsWith("http"))
}
```

## 🚀 Deployment

### 1. Build de Release

```bash
# Generar APK de release
./gradlew assembleRelease

# Generar bundle para Play Store
./gradlew bundleRelease
```

### 2. Verificar APK

```bash
# Verificar tamaño del APK
ls -lh app/build/outputs/apk/release/app-release.apk

# Verificar contenido del APK
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

### 3. Testing de Release

```bash
# Instalar APK de release
adb install app/build/outputs/apk/release/app-release.apk

# Verificar que funciona correctamente
adb shell am start -n com.ingeneo.scanprefrio/.MainActivity
```

## 📊 Monitoreo Post-Migración

### 1. Logs de Migración

```kotlin
// Agregar logs para monitorear migración
Log.d("Migration", "Iniciando migración de base de datos...")
Log.d("Migration", "Versión anterior: $oldVersion")
Log.d("Migration", "Versión nueva: $newVersion")
Log.d("Migration", "Migración completada exitosamente")
```

### 2. Métricas de Performance

```kotlin
// Monitorear tiempo de migración
val startTime = System.currentTimeMillis()
// ... migración ...
val endTime = System.currentTimeMillis()
Log.d("Migration", "Tiempo de migración: ${endTime - startTime}ms")
```

### 3. Verificación de Datos

```kotlin
// Verificar que los datos se migraron correctamente
suspend fun verifyMigration() {
    val records = repository.getAllRecords()
    val clientes = repository.getAllClientes()
    val tipos = repository.getAllTiposFlor()
    val variedades = repository.getAllVariedades()
    
    Log.d("Migration", "Registros: ${records.size}")
    Log.d("Migration", "Clientes: ${clientes.size}")
    Log.d("Migration", "Tipos: ${tipos.size}")
    Log.d("Migration", "Variedades: ${variedades.size}")
}
```

## 🛠️ Rollback Plan

### 1. Backup de Datos

```kotlin
// Antes de migrar, hacer backup
suspend fun backupDatabase() {
    val records = repository.getAllRecords()
    val backup = records.map { it.copy() }
    // Guardar backup en SharedPreferences o archivo
}
```

### 2. Rollback de Configuración

```kotlin
// Si algo sale mal, restaurar configuración anterior
fun rollbackConfiguration() {
    // Restaurar valores anteriores en AppConfig
    // O cambiar entorno en EnvironmentConfig
}
```

### 3. Rollback de Base de Datos

```kotlin
// Si la migración falla, restaurar versión anterior
fun rollbackDatabase() {
    // Eliminar base de datos y restaurar desde backup
    // O usar fallbackToDestructiveMigration()
}
```

## 📋 Checklist de Migración

### Antes de Migrar
- [ ] Hacer backup de la base de datos
- [ ] Probar cambios en entorno de desarrollo
- [ ] Verificar que todas las dependencias son compatibles
- [ ] Documentar cambios realizados

### Durante la Migración
- [ ] Incrementar versiones de base de datos
- [ ] Actualizar configuraciones
- [ ] Agregar migraciones necesarias
- [ ] Actualizar dependencias

### Después de Migrar
- [ ] Probar funcionalidad completa
- [ ] Verificar logs de migración
- [ ] Monitorear performance
- [ ] Documentar resultados

## 🆘 Troubleshooting

### Problemas Comunes

#### 1. Error de Compilación
```bash
# Limpiar cache de Gradle
./gradlew clean
rm -rf .gradle
./gradlew build
```

#### 2. Error de Migración
```kotlin
// Usar fallback para desarrollo
.fallbackToDestructiveMigration()
```

#### 3. Error de API
```kotlin
// Verificar configuración de red
// Revisar logs de Retrofit
// Verificar certificados SSL
```

#### 4. Error de Camera
```kotlin
// Verificar permisos
// Actualizar CameraX
// Verificar compatibilidad de dispositivo
```

---

*Guía de migración v1.0.0 - Enero 2024* 