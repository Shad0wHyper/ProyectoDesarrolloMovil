package com.developers.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlmacenScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Control de Almacén",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StockStatCard(
                modifier = Modifier.weight(1f),
                label = "Insumos Críticos",
                value = "12",
                color = Color(0xFFF44336)
            )
            StockStatCard(
                modifier = Modifier.weight(1f),
                label = "Próximos a Vencer",
                value = "5",
                color = Color(0xFFFFA000)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Materias Primas",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(getInsumosEjemplo()) { insumo ->
                InsumoCard(insumo)
            }
        }
    }
}

@Composable
fun StockStatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
fun InsumoCard(insumo: Insumo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(insumo.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = insumo.color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(insumo.nombre, fontWeight = FontWeight.Bold)
                Text("Stock: ${insumo.cantidad} ${insumo.unidad}", color = Color.Gray, fontSize = 14.sp)
            }
            CircularProgressIndicator(
                progress = { insumo.nivel },
                modifier = Modifier.size(32.dp),
                color = insumo.color,
                strokeWidth = 4.dp,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}

data class Insumo(val nombre: String, val cantidad: Double, val unidad: String, val nivel: Float, val color: Color)

fun getInsumosEjemplo() = listOf(
    Insumo("Harina de Trigo", 50.0, "kg", 0.8f, Color(0xFF4CAF50)),
    Insumo("Mantequilla", 5.0, "kg", 0.2f, Color(0xFFF44336)),
    Insumo("Levadura Seca", 2.5, "kg", 0.5f, Color(0xFFFFA000)),
    Insumo("Azúcar Glass", 10.0, "kg", 0.9f, Color(0xFF4CAF50))
)
