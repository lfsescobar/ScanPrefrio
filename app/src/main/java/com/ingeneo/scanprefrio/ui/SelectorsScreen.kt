package com.ingeneo.scanprefrio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.util.Log
import com.ingeneo.scanprefrio.api.*
import com.ingeneo.scanprefrio.api.SelectorsRetrofitClient
import com.ingeneo.scanprefrio.database.ScanDatabase
import com.ingeneo.scanprefrio.database.ScanRecord
import com.ingeneo.scanprefrio.database.ScanRepository
import com.ingeneo.scanprefrio.database.ClienteEntity
import com.ingeneo.scanprefrio.database.TipoFlorEntity
import com.ingeneo.scanprefrio.database.VariedadEntity
import com.ingeneo.scanprefrio.sync.SyncWorker
import com.ingeneo.scanprefrio.sync.SelectorsSyncService
import com.ingeneo.scanprefrio.config.AppConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SelectorsScreen(
    stationCode: String,
    stationScanTime: Long,
    onBackClick: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { ScanDatabase.getDatabase(context) }
    val repository = remember { ScanRepository(database.scanDao()) }
    
    // Estados para los datos de los selectores
    var clientes by remember { mutableStateOf<List<ClienteEntity>>(emptyList()) }
    var tiposFlor by remember { mutableStateOf<List<TipoFlorEntity>>(emptyList()) }
    var variedades by remember { mutableStateOf<List<VariedadEntity>>(emptyList()) }
    
    // Estados para las selecciones
    var selectedCliente by remember { mutableStateOf<ClienteEntity?>(null) }
    var selectedTipoFlor by remember { mutableStateOf<TipoFlorEntity?>(null) }
    var selectedVariedad by remember { mutableStateOf<VariedadEntity?>(null) }
    
    // Estados para controlar los diálogos de selección
    var showClienteDialog by remember { mutableStateOf(false) }
    var showTipoFlorDialog by remember { mutableStateOf(false) }
    var showVariedadDialog by remember { mutableStateOf(false) }
    
    // Estados de carga
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Estado para el diálogo de éxito
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Formateador de fecha
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // Cargar datos de las APIs
    LaunchedEffect(key1 = Unit) {
        try {
            isLoading = true
            
            // Cargar datos locales
            clientes = SelectorsSyncService.getLocalClientes(context)
            tiposFlor = SelectorsSyncService.getLocalTiposFlor(context)
            variedades = SelectorsSyncService.getLocalVariedades(context)
            
            // Si no hay datos locales, intentar sincronizar
            if (clientes.isEmpty() || tiposFlor.isEmpty() || variedades.isEmpty()) {
                Log.d("SelectorsScreen", "📋 No hay datos locales, sincronizando...")
                val syncSuccess = SelectorsSyncService.syncSelectorsData(context)
                if (syncSuccess) {
                    Log.d("SelectorsScreen", "✅ Sincronización exitosa, recargando datos...")
                    // Recargar datos locales después de sincronizar
                    clientes = SelectorsSyncService.getLocalClientes(context)
                    tiposFlor = SelectorsSyncService.getLocalTiposFlor(context)
                    variedades = SelectorsSyncService.getLocalVariedades(context)
                } else {
                    Log.e("SelectorsScreen", "❌ Error en sincronización")
                    errorMessage = "No se pudieron cargar los datos. Verifique su conexión a internet."
                }
            } else {
                Log.d("SelectorsScreen", "📋 Datos locales cargados: ${clientes.size} clientes, ${tiposFlor.size} tipos, ${variedades.size} variedades")
            }
            
            isLoading = false
        } catch (e: Exception) {
            Log.e("SelectorsScreen", "❌ Error al cargar datos", e)
            errorMessage = "Error al cargar datos: ${e.message}"
            isLoading = false
        }
    }
    
    // Función para guardar el registro
    val saveRecord = {
        scope.launch {
            val currentTime = System.currentTimeMillis()
            val scanRecord = ScanRecord(
                qrPrefrio = stationCode,
                dateTimePrefrio = dateFormat.format(Date(stationScanTime)),
                qrMercancia = "${selectedCliente?.nombre ?: ""} - ${selectedTipoFlor?.nombre ?: ""} - ${selectedVariedad?.nombre ?: ""}",
                dateTimeMercancia = dateFormat.format(Date(currentTime)),
                segDif = (currentTime - stationScanTime) / 1000,
                sendApi = 0
            )
            repository.insertScanRecord(scanRecord)
            SyncWorker.syncNow(context)
            showSuccessDialog = true
        }
        Unit
    }
    
    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.navigate("station_scan") {
                    popUpTo("station_scan") { inclusive = true }
                }
            },
            title = { Text("Éxito") },
            text = { Text("Registro guardado con éxito") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate("station_scan") {
                            popUpTo("station_scan") { inclusive = true }
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(AppConfig.Colors.PRIMARY_GREEN))
                    .padding(top = 50.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SELECCIONAR DATOS",
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
            }
        },
        bottomBar = {
            // Barra inferior verde con botón de guardar
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
                    // Botón "Guardar Registro" centrado
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = saveRecord,
                            enabled = selectedCliente != null && selectedTipoFlor != null && selectedVariedad != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedCliente != null && selectedTipoFlor != null && selectedVariedad != null) 
                                    Color.White else Color.Gray
                            )
                        ) {
                            Text(
                                text = "GUARDAR",
                                                            color = if (selectedCliente != null && selectedTipoFlor != null && selectedVariedad != null) 
                                Color(AppConfig.Colors.PRIMARY_GREEN) else Color.White,
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando datos...")
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Estación: $stationCode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Selector de Cliente
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Cliente",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showClienteDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(AppConfig.Colors.GRAY_BUTTON)
                                    )
                                ) {
                                    Text(
                                        text = selectedCliente?.nombre ?: "Seleccionar Cliente",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // Selector de Tipo de Flor
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Tipo de Flor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showTipoFlorDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(AppConfig.Colors.GRAY_BUTTON)
                                    )
                                ) {
                                    Text(
                                        text = selectedTipoFlor?.nombre ?: "Seleccionar Tipo de Flor",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // Selector de Variedad
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Variedad",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showVariedadDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(AppConfig.Colors.GRAY_BUTTON)
                                    )
                                ) {
                                    Text(
                                        text = selectedVariedad?.nombre ?: "Seleccionar Variedad",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo para seleccionar Cliente
    if (showClienteDialog) {
        AlertDialog(
            onDismissRequest = { showClienteDialog = false },
            modifier = Modifier.fillMaxSize(),
            title = { 
                Text(
                    "Seleccionar Cliente",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(clientes) { cliente ->
                        Button(
                            onClick = {
                                selectedCliente = cliente
                                showClienteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(AppConfig.Colors.GRAY_BUTTON)
                            )
                        ) {
                            Text(
                                text = cliente.nombre,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                color = Color.White
                            )
                        }
                        Divider(color = Color.LightGray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showClienteDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(AppConfig.Colors.GRAY_BUTTON)
                    )
                ) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(AppConfig.Colors.DARK_BACKGROUND)
        )
    }
    
    // Diálogo para seleccionar Tipo de Flor
    if (showTipoFlorDialog) {
        AlertDialog(
            onDismissRequest = { showTipoFlorDialog = false },
            modifier = Modifier.fillMaxSize(),
            title = { 
                Text(
                    "Seleccionar Tipo de Flor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tiposFlor) { tipo ->
                        Button(
                            onClick = {
                                selectedTipoFlor = tipo
                                showTipoFlorDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF666666)
                            )
                        ) {
                            Text(
                                text = tipo.nombre,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                color = Color.White
                            )
                        }
                        Divider(color = Color.LightGray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTipoFlorDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF666666)
                    )
                ) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(AppConfig.Colors.DARK_BACKGROUND)
        )
    }
    
    // Diálogo para seleccionar Variedad
    if (showVariedadDialog) {
        AlertDialog(
            onDismissRequest = { showVariedadDialog = false },
            modifier = Modifier.fillMaxSize(),
            title = { 
                Text(
                    "Seleccionar Variedad",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(variedades) { variedad ->
                        Button(
                            onClick = {
                                selectedVariedad = variedad
                                showVariedadDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF666666)
                            )
                        ) {
                            Text(
                                text = variedad.nombre,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                color = Color.White
                            )
                        }
                        Divider(color = Color.LightGray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVariedadDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF666666)
                    )
                ) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(AppConfig.Colors.DARK_BACKGROUND)
        )
    }
} 