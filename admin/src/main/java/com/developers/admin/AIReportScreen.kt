package com.developers.admin

import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastErrorColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.developers.admin.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.coroutines.launch

// --- 1. MODELOS DE DATOS PARA LA API ---
data class PrediccionRequest(
    val fecha: String,
    val tipo_pan: String,
    val temp_max: Double,
    val temp_min: Double,
    val lluvia_mm: Double,
    val lag_1: Double,
    val lag_7: Double
)



data class DatosPrediccion(
    val fecha: String,
    val tipo_pan: String,
    val prediccion_ventas: Double,
    val orden_produccion: Int,
    val harina_necesaria_kg: Double,
    val origen_motor: String
)
data class PrediccionResponse(val status: String, val datos: DatosPrediccion)

// --- 2. INTERFAZ DE RETROFIT ---
interface PanAppApi {
    @POST("predecir")
    suspend fun obtenerPrediccion(@Body request: PrediccionRequest): PrediccionResponse
}

// --- 3. CLIENTE DE RED ---
object RetrofitClient {
    // OUR VERY OWN WEB SERVER
    private const val BASE_URL = "https://panapp-ai.onrender.com/"

    val api: PanAppApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PanAppApi::class.java)
    }
}

// --- 4. LA PANTALLA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA)
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val topBarColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    // Variables de estado para la red
    var isLoading by remember { mutableStateOf(true) }
    var prediccion by remember { mutableStateOf<DatosPrediccion?>(null) }
    var errorRed by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        try {
            // El Mago de Oz espera exactamente estos parámetros
            // Le mandamos los 7 parámetros que exige FastAPI
            val request = PrediccionRequest(
                fecha = "2026-10-15",
                tipo_pan = "Bolillo",
                temp_max = 28.5,
                temp_min = 12.0,
                lluvia_mm = 0.0,
                lag_1 = 500.0,
                lag_7 = 480.0
            )
            val response = RetrofitClient.api.obtenerPrediccion(request)
            prediccion = response.datos
        } catch (e: Exception) {
            errorRed = localizedText(TextR.string.pan_connection_error, e.localizedMessage.orEmpty())
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_ai_prediction_report), fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.admin_back), tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->

        if (isLoading) {
            // Muestra una rueda de carga mientras Render responde
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF673AB7))
            }
        } else if (errorRed != null) {
            // Si algo falla, mostramos el error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorRed!!, color = contrastErrorColor(Color.Red), modifier = Modifier.padding(16.dp))
            }
        } else {
            // ¡Ya tenemos los datos del servidor!
            val datosIA = prediccion!!

            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        localizedText(TextR.string.pan_neural_analysis),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF673AB7),
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    PredictionCard(
                        title = localizedText(TextR.string.pan_estimated_sales, datosIA.tipo_pan),
                        value = localizedText(TextR.string.pan_pieces, datosIA.prediccion_ventas), // Viene de FastAPI
                        description = localizedText(TextR.string.pan_weather_date, datosIA.fecha),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = Color(0xFF4CAF50),
                        isDarkMode = isDarkMode
                    )
                }

                item {
                    PredictionCard(
                        title = localizedText(TextR.string.pan_stock_request),
                        value = localizedText(TextR.string.pan_flour_kg, datosIA.harina_necesaria_kg), // Viene de FastAPI
                        description = localizedText(TextR.string.pan_prediction_engine, datosIA.origen_motor),
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFF2196F3),
                        isDarkMode = isDarkMode,
                        action = {
                            Button(
                                onClick = {
                                    val mensaje = localizedText(TextR.string.pan_ai_order_message, datosIA.tipo_pan, datosIA.harina_necesaria_kg)
                                    val uri = Uri.parse("whatsapp://send?text=${Uri.encode(mensaje)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.admin_error_open_whatsapp), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon( Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text( stringResource(R.string.admin_order_via_whatsapp), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                            }
                        }
                    )
                }

                item {
                    PredictionCard(
                        title = localizedText(TextR.string.pan_suggested_production),
                        value = localizedText(TextR.string.pan_pieces, datosIA.orden_produccion), // Viene de FastAPI
                        description = localizedText(TextR.string.pan_waste_optimization),
                        icon = Icons.Default.PrecisionManufacturing,
                        color = Color(0xFFFFA000),
                        isDarkMode = isDarkMode
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF311B92).copy(alpha = 0.3f) else Color(0xFFEDE7F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF673AB7))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.admin_cloud_connection), fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF673AB7))
                            }
                            Text(
                                localizedText(TextR.string.pan_render_source),
                                style = MaterialTheme.typography.bodySmall,
                                color = contrastTextColor(Color.Gray, secondary = true)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    color: Color,
    isDarkMode: Boolean,
    action: @Composable (() -> Unit)? = null
) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodySmall, color = contrastTextColor(Color.Gray, secondary = true))
                    Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = contrastTextColor(Color.DarkGray, secondary = true, darkBackground = isDarkMode))
                }
            }
            action?.invoke()
        }
    }
}