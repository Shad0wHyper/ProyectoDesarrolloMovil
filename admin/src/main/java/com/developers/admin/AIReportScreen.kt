package com.developers.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reporte de Predicciones IA", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Análisis Basado en Red Neuronal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF673AB7),
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                PredictionCard(
                    title = "Ventas Estimadas Hoy",
                    value = "$1,450.00",
                    description = "Se espera un incremento del 15% debido a festividad local.",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = Color(0xFF4CAF50)
                )
            }

            item {
                PredictionCard(
                    title = "Stock a Solicitar",
                    value = "Harina: 50kg, Azúcar: 10kg",
                    description = "Basado en el ritmo de venta de los últimos 7 días.",
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0xFF2196F3),
                    action = {
                        Button(
                            onClick = {
                                val mensaje = "Hola, me gustaría realizar el siguiente pedido basado en las predicciones de stock de hoy:\n- Harina: 50kg\n- Azúcar: 10kg"
                                val uri = Uri.parse("whatsapp://send?text=${Uri.encode(mensaje)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to web API if whatsapp:// fails or app not installed
                                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(mensaje)}")
                                    }
                                    try {
                                        context.startActivity(webIntent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pedir vía WhatsApp")
                        }
                    }
                )
            }

            item {
                PredictionCard(
                    title = "Producción Sugerida",
                    value = "200 Panes, 80 Bollería",
                    description = "Optimización para reducir desperdicios al mínimo (0.5%).",
                    icon = Icons.Default.PrecisionManufacturing,
                    color = Color(0xFFFFA000)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF673AB7))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confianza del Modelo: 94.2%", fontWeight = FontWeight.Bold, color = Color(0xFF673AB7))
                        }
                        Text(
                            "Último entrenamiento: Hace 2 horas",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
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
    action: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
            }
            action?.invoke()
        }
    }
}
