package com.ingeneo.scanprefrio

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.ingeneo.scanprefrio.database.ScanDatabase
import com.ingeneo.scanprefrio.database.ScanRecord
import com.ingeneo.scanprefrio.database.ScanRepository
import com.ingeneo.scanprefrio.sync.SyncWorker
import com.ingeneo.scanprefrio.sync.SelectorsSyncService
import com.ingeneo.scanprefrio.ui.SelectorsScreen
import com.ingeneo.scanprefrio.ui.theme.ScanPrefrioTheme
import com.ingeneo.scanprefrio.config.AppConfig
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Log de inicio de aplicación para depuración
        Log.e(AppConfig.LogTags.APP_STARTUP, "================= APLICACIÓN INICIADA =================")

        // Iniciar la sincronización periódica
        SyncWorker.scheduleSync(this)
        
        // Sincronizar datos de selectores al iniciar la aplicación
        lifecycleScope.launch {
            try {
                Log.d(AppConfig.LogTags.APP_STARTUP, "🔄 Sincronizando datos de selectores al iniciar...")
                SelectorsSyncService.syncSelectorsData(this@MainActivity)
                Log.d(AppConfig.LogTags.APP_STARTUP, "✅ Sincronización de selectores completada")
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.APP_STARTUP, "❌ Error al sincronizar selectores al iniciar", e)
            }
        }

        setContent {
            ScanPrefrioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// Rutas de navegación
sealed class Screen(val route: String) {
    object StationScan : Screen("station_scan")
    object MerchandiseScan : Screen("merchandise_scan")
    object Selectors : Screen("selectors") // Nueva ruta para los selectores
    object RegisterList : Screen("register_list") // Nueva ruta para la lista de registros
}

// Función para validar QR de estación
fun isValidQrCode(code: String): Boolean {
    // Verifica que solo contenga texto (letras, espacios o dígitos) y que tenga máximo 6 caracteres
    return code.length <= AppConfig.QRValidation.MAX_STATION_CODE_LENGTH && 
           code.all { it.isLetterOrDigit() || it.isWhitespace() }
}

// Función para validar QR de mercancía
fun isValidMerchandiseQrCode(code: String): Boolean {
    // Solo verifica que sea alfanumérico (letras y números)
    return code.isNotEmpty() && code.all { it.isLetterOrDigit() }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Inicializar la base de datos y el repositorio
    val database = remember { ScanDatabase.getDatabase(context) }
    val repository = remember { ScanRepository(database.scanDao()) }

    // Variable para almacenar el código de la estación escaneada
    var lastStationCode by remember { mutableStateOf("") }
    // Variables para almacenar los tiempos de escaneo
    var stationScanTime by remember { mutableStateOf(0L) }
    var merchandiseScanTime by remember { mutableStateOf(0L) }

    // Formateador de fecha y hora
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    NavHost(navController = navController, startDestination = Screen.StationScan.route) {
        composable(Screen.StationScan.route) {
            ScanScreen(
                title = "ESCANEAR PREFRIO",
                validateQr = { code ->
                    isValidQrCode(code)
                },
                onValidQrDetected = { qrCode ->
                    // El flash se muestra antes de esta llamada en ScanScreen
                    // Guardar el código de la estación y el tiempo de escaneo
                    lastStationCode = qrCode
                    stationScanTime = System.currentTimeMillis()

                    Log.d("ScanPrefrio", "Prefrio escaneado: $qrCode en ${dateFormat.format(Date(stationScanTime))}")
                    navController.navigate(Screen.Selectors.route)
                },
                errorMessage = "Código QR no válido. Debe ser texto de máximo 6 caracteres.",
                initialDelay = 0, // Sin retraso inicial para la primera pantalla
                showCancelButton = false, // No mostrar botón de cancelar en la primera pantalla
                navController = navController // Pasar el navController a ScanScreen
            )
        }
        composable(Screen.MerchandiseScan.route) {
            // Estado para el diálogo de éxito
            var showSuccessDialog by remember { mutableStateOf(false) }
            var scannedMerchandiseCode by remember { mutableStateOf("") }

            // Si se muestra el diálogo, mostrar la alerta de éxito
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showSuccessDialog = false
                        // Regresar a la pantalla inicial después de cerrar el diálogo
                        navController.navigate(Screen.StationScan.route) {
                            // Limpiar el back stack para que no se pueda volver a esta pantalla con el botón Back
                            popUpTo(Screen.StationScan.route) { inclusive = true }
                        }
                    },
                    title = { Text("Éxito") },
                    text = { Text("Registro guardado con éxito") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSuccessDialog = false
                                // Regresar a la pantalla inicial después de hacer clic en Aceptar
                                navController.navigate(Screen.StationScan.route) {
                                    popUpTo(Screen.StationScan.route) { inclusive = true }
                                }
                            }
                        ) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            ScanScreen(
                title = "ESCANEAR FLOR",
                validateQr = { code ->
                    // Añadimos la validación para ignorar si es el mismo código de la estación
                    isValidMerchandiseQrCode(code) && code != lastStationCode
                },
                onValidQrDetected = { qrCode ->
                    // Guardar el código de la mercancía y el tiempo de escaneo
                    scannedMerchandiseCode = qrCode
                    merchandiseScanTime = System.currentTimeMillis()

                    // Calcular la diferencia en segundos
                    val diffMillis = merchandiseScanTime - stationScanTime
                    val diffSeconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis)

                    // Crear una tabla en el log
                    val logSeparator = "+-----------------+------------------------+------------------+------------------------+--------------+"
                    val logHeader =    "| Prefrio         | Fecha/Hora Prefrio      | Mercancía        | Fecha/Hora Mercancía   | Dif. (segs)  |"
                    val logData = String.format(
                        "| %-15s | %-22s | %-16s | %-22s | %-12d |",
                        lastStationCode,
                        dateFormat.format(Date(stationScanTime)),
                        qrCode,
                        dateFormat.format(Date(merchandiseScanTime)),
                        diffSeconds
                    )

                    // Mostrar la tabla en LogCat
                    Log.d("ScanPrefrio", "\n$logSeparator\n$logHeader\n$logSeparator\n$logData\n$logSeparator")

                    // Guardar el registro en la base de datos
                    scope.launch {
                        val scanRecord = ScanRecord(
                            qrPrefrio = lastStationCode,
                            dateTimePrefrio = dateFormat.format(Date(stationScanTime)),
                            qrMercancia = qrCode,
                            dateTimeMercancia = dateFormat.format(Date(merchandiseScanTime)),
                            segDif = diffSeconds,
                            sendApi = 0 // 0 = no enviado
                        )
                        val id = repository.insertScanRecord(scanRecord)
                        Log.d("ScanPrefrio", "Registro guardado en la base de datos con ID: $id")

                        // Iniciar sincronización inmediata tras guardar el registro
                        SyncWorker.syncNow(context)
                    }

                    // Mostrar diálogo de éxito cuando se escanea un código válido
                    showSuccessDialog = true
                    // La navegación a la pantalla inicial se maneja en el diálogo
                },
                errorMessage = "Código QR no válido. Debe ser diferente al de la estación y contener solo caracteres alfanuméricos.",
                initialDelay = 1500, // Retraso de 1.5 segundos antes de comenzar a escanear
                showCancelButton = true, // Mostrar botón de cancelar en la segunda pantalla
                onCancelClick = {
                    // Navegar de regreso a la pantalla inicial
                    navController.navigate(Screen.StationScan.route) {
                        popUpTo(Screen.StationScan.route) { inclusive = true }
                    }
                },
                navController = navController // Pasar el navController a ScanScreen
            )
        }
        // Nueva pantalla para los selectores
        composable(Screen.Selectors.route) {
            SelectorsScreen(
                stationCode = lastStationCode,
                stationScanTime = stationScanTime,
                onBackClick = {
                    navController.navigate(Screen.StationScan.route) {
                        popUpTo(Screen.StationScan.route) { inclusive = true }
                    }
                },
                navController = navController
            )
        }
        // Nueva pantalla para mostrar los registros enviados
        composable(Screen.RegisterList.route) {
            RegistrosScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ScanScreen(
    title: String,
    validateQr: (String) -> Boolean,
    onValidQrDetected: (String) -> Unit,
    errorMessage: String,
    initialDelay: Long = 0, // Nuevo parámetro para el retraso inicial
    modifier: Modifier = Modifier,
    showCancelButton: Boolean = false, // Nuevo parámetro para mostrar el botón de cancelar
    onCancelClick: () -> Unit = {}, // Nuevo parámetro para manejar clic en cancelar
    navController: NavController // Añadir navController como parámetro
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Crear MediaPlayer para el sonido de beep
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.beep) }

    // Estado para controlar el efecto de flash
    var showFlash by remember { mutableStateOf(false) }

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )

    // Estado para controlar el diálogo de error
    var showErrorDialog by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf("") }

    // Estado para habilitar/deshabilitar el escáner
    var isScanningEnabled by remember { mutableStateOf(initialDelay == 0L) }

    // Si hay un retraso inicial, esperar ese tiempo antes de habilitar el escáner
    LaunchedEffect(key1 = Unit) {
        if (initialDelay > 0) {
            // Mostrar indicador de carga mientras esperamos
            isScanningEnabled = false
            kotlinx.coroutines.delay(initialDelay)
            isScanningEnabled = true
        }
    }

    // Solicitar permiso si aún no se ha concedido
    LaunchedEffect(key1 = true) {
        if (!hasCamPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Función para procesar código QR escaneado
    val processQrCode: (String) -> Unit = { code ->
        // Solo procesar si el escáner está habilitado
        if (isScanningEnabled) {
            // Reproducir sonido al detectar cualquier código QR
            mediaPlayer.start()

            if (validateQr(code)) {
                // Mostrar el flash antes de llamar a onValidQrDetected
                showFlash = true

                // Ocultar el flash después de un breve periodo
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(300) // Duración del flash en milisegundos
                    showFlash = false
                    onValidQrDetected(code)
                }
            } else {
                dialogErrorMessage = errorMessage
                showErrorDialog = true
            }
        }
    }

    // Diálogo de error
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error de validación") },
            text = { Text(dialogErrorMessage) },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Limpiar MediaPlayer cuando se desmonte la composición
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            // Barra superior verde con el título
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50))
                    .padding(top = 50.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Título centrado (sin depender de Row)
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Contenedor para el botón de cancelar a la derecha
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Botón de cancelar (X) a la derecha, solo si showCancelButton es true
                    if (showCancelButton) {
                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancelar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Barra inferior verde con dos botones - ajustada para subir los botones
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(AppConfig.Colors.PRIMARY_GREEN))
                    .padding(vertical = 16.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón "Consultar Registro" (lado izquierdo) en un Box para centrado
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                navController.navigate(Screen.RegisterList.route)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Text(
                                text = "CONSULTAR",
                                color = Color(AppConfig.Colors.PRIMARY_GREEN),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Botón "Salir" (lado derecho) en un Box para centrado - CAMBIADO A ROJO
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                (context as? Activity)?.finishAffinity()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935) // Color rojo
                            )
                        ) {
                            Text(
                                text = "SALIR",
                                color = Color.White, // Texto en blanco para contrastar
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Contenedor para la cámara
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCamPermission) {
                        if (!isScanningEnabled) {
                            // Mostrar indicador de carga mientras el escáner está deshabilitado
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Preparando cámara...", style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        // Mostrar vista previa de la cámara si se concedió el permiso
                        CameraPreview(onQrCodeDetected = processQrCode)
                    } else {
                        // Mostrar mensaje si no hay permiso
                        Text("Se necesita permiso de cámara para escanear.")
                    }
                }
            }

            // Efecto de flash superpuesto cuando showFlash es true
            AnimatedVisibility(
                visible = showFlash,
                enter = fadeIn(animationSpec = tween(100)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }
}

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraPreview(onQrCodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewUseCase by remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewUseCase) {
        val cameraProvider = cameraProviderFuture.get()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image
            if (image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegrees)
                scanner.process(processImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (rawValue != null && rawValue != lastScannedCode) {
                                lastScannedCode = rawValue
                                Log.d("QRCodeScan", "Código QR detectado: $rawValue")
                                // Llamar al callback con el código QR detectado
                                onQrCodeDetected(rawValue)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("QRCodeScan", "Error al escanear", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()
            previewUseCase?.let {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    it,
                    imageAnalysis
                )
            }
        } catch (exc: Exception) {
            Log.e("CameraPreview", "Error al vincular casos de uso", exc)
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            previewUseCase = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Componente para mostrar un registro individual
@Composable
fun RecordItem(record: ScanRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Estado del registro (enviado o pendiente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (record.sendApi == 1) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (record.sendApi == 1) "Enviado" else "Pendiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (record.sendApi == 1) Color.Green else Color.Red
                )
            }

            Text(
                text = "Estación Prefrio: ${record.qrPrefrio}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fecha: ${record.dateTimePrefrio}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Datos de Mercancía: ${record.qrMercancia}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fecha: ${record.dateTimeMercancia}",
                style = MaterialTheme.typography.bodyMedium
            )
            // Se eliminó la sección de diferencia de tiempo
        }
    }
}

@Composable
fun RegistrosScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { ScanDatabase.getDatabase(context) }
    val repository = remember { ScanRepository(database.scanDao()) }

    // Estado para almacenar los registros
    var records by remember { mutableStateOf<List<ScanRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    // Cargar todos los registros (pendientes y últimos 10 enviados)
    LaunchedEffect(key1 = Unit) {
        scope.launch {
            records = repository.getPendingAndRecentSyncedRecords()
            isLoading = false
        }
    }
    
    // Función para sincronizar datos de selectores
    val syncSelectorsData = {
        scope.launch {
            isSyncing = true
            syncMessage = null
            try {
                val success = SelectorsSyncService.syncSelectorsData(context)
                syncMessage = if (success) {
                    "Datos sincronizados exitosamente"
                } else {
                    "Error al sincronizar datos"
                }
            } catch (e: Exception) {
                syncMessage = "Error: ${e.message}"
            } finally {
                isSyncing = false
            }
        }
        Unit
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50))
                    .padding(top = 50.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REGISTROS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Botón de sincronización a la derecha
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = syncSelectorsData,
                        enabled = !isSyncing,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sincronizar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Añadir espacio en blanco para evitar solapamiento con botones del sistema
            Spacer(modifier = Modifier.height(80.dp))
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Mostrar mensaje de sincronización si existe
                syncMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.contains("exitosamente")) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                if (isLoading) {
                    // Mostrar indicador de carga
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (records.isEmpty()) {
                    // Mostrar mensaje si no hay registros
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay registros",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    // Mostrar lista de registros
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(records) { record ->
                            RecordItem(record)
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    ScanPrefrioTheme {
        ScanScreen(
            title = "Escanee la estación",
            validateQr = { true },
            onValidQrDetected = {},
            errorMessage = "Código QR no válido.",
            navController = rememberNavController()
        )
    }
}
