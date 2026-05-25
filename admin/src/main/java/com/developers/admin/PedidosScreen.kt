package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PedidosScreen() {
    val context = LocalContext.current
    
    // 1. Estado para Pedido Seleccionado y Visibilidad
    var showBottomSheet by remember { mutableStateOf(false) }
    var pedidoSeleccionado by remember { mutableStateOf<Pedido?>(null) }
    
    // Lista mutable para simular actualización de estado
    val listaPedidos = remember { 
        mutableStateListOf<Pedido>().apply { addAll(getPedidosEjemplo()) } 
    }

    // 2. Estado para Filtros
    var filtroSeleccionado by remember { mutableStateOf("Pendientes") }
    
    val pedidosFiltrados = remember(filtroSeleccionado, listaPedidos.size, listaPedidos.map { it.estado }) {
        when (filtroSeleccionado) {
            "Pendientes" -> listaPedidos.filter { it.estado == "Pendiente" || it.estado == "En Camino" }
            "En Proceso" -> listaPedidos.filter { it.estado == "En Proceso" }
            "Completados" -> listaPedidos.filter { it.estado == "Entregado" }
            else -> listaPedidos
        }
    }

    if (showBottomSheet && pedidoSeleccionado != null) {
        DetallePedidoBottomSheet(
            pedido = pedidoSeleccionado!!,
            onDismissRequest = { showBottomSheet = false },
            onConfirmarRecepcion = { pedido ->
                // 3. Lógica de Confirmar Recepción
                val index = listaPedidos.indexOfFirst { it.id == pedido.id }
                if (index != -1) {
                    listaPedidos[index] = pedido.copy(
                        estado = "Entregado",
                        estadoColor = Color(0xFF4CAF50)
                    )
                }
                Toast.makeText(context, "Pedido ${pedido.id} marcado como recibido", Toast.LENGTH_SHORT).show()
                showBottomSheet = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pedidos Activos",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                color = Color(0xFF2196F3).copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    "${listaPedidos.size} hoy",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Filter Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filtroSeleccionado == "Pendientes", 
                onClick = { filtroSeleccionado = "Pendientes" }, 
                label = { Text("Pendientes") }
            )
            FilterChip(
                selected = filtroSeleccionado == "En Proceso", 
                onClick = { filtroSeleccionado = "En Proceso" }, 
                label = { Text("En Proceso") }
            )
            FilterChip(
                selected = filtroSeleccionado == "Completados", 
                onClick = { filtroSeleccionado = "Completados" }, 
                label = { Text("Completados") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pedidosFiltrados) { pedido ->
                PedidoCard(
                    pedido = pedido, 
                    onClick = { 
                        pedidoSeleccionado = pedido
                        showBottomSheet = true 
                    }
                )
            }
        }
    }
}

@Composable
fun PedidoCard(pedido: Pedido, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#${pedido.id}", fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(pedido.hora, fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pedido.cliente, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = pedido.estadoColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        pedido.estado,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = pedido.estadoColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(pedido.detalles, color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$ ${pedido.total}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2196F3))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Detalles")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

data class ArticuloPedido(val nombre: String, val cantidad: String)

data class Pedido(
    val id: String, 
    val cliente: String, 
    val detalles: String, 
    val total: String, 
    val hora: String,
    val proveedor: String,
    val estado: String,
    val estadoColor: Color,
    val articulos: List<ArticuloPedido>
)

fun getPedidosEjemplo() = listOf(
    Pedido(
        "1001", "Juan Pérez", "2x Croissant, 1x Baguette", "6.80", "10:15 AM",
        "Harinas del Sol", "En Camino", Color(0xFF0D47A1),
        listOf(ArticuloPedido("Croissant", "2 und"), ArticuloPedido("Baguette", "1 und"))
    ),
    Pedido(
        "1002", "María García", "4x Muffin de Chocolate", "12.80", "10:20 AM",
        "Distribuidora Láctea", "Pendiente", Color(0xFFFFA000),
        listOf(ArticuloPedido("Muffin Chocolate", "4 und"))
    ),
    Pedido(
        "1003", "Carlos Ruiz", "1x Pan Integral, 2x Donas", "5.50", "10:30 AM",
        "Panadería Central", "En Proceso", Color(0xFF2196F3),
        listOf(ArticuloPedido("Pan Integral", "1 und"), ArticuloPedido("Donas", "2 und"))
    )
)
