package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePedidoBottomSheet(
    pedido: Pedido,
    onDismissRequest: () -> Unit,
    onConfirmarRecepcion: (Pedido) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        DetallePedidoContent(
            pedido = pedido,
            onDismissRequest = onDismissRequest,
            onConfirmarRecepcion = onConfirmarRecepcion
        )
    }
}

@Composable
fun DetallePedidoContent(
    pedido: Pedido,
    onDismissRequest: () -> Unit,
    onConfirmarRecepcion: (Pedido) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Detalle de Pedido",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "ID: ORD-${pedido.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = pedido.estadoColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "• ${pedido.estado}",
                        color = pedido.estadoColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tarjeta del Proveedor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = pedido.proveedor, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Suministros de Panadería Premium",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Contactar Proveedor",
                        color = Color(0xFF2196F3),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            Toast.makeText(context, "Contactando a ${pedido.proveedor}...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ARTÍCULOS EN PEDIDO
        Text(
            text = "ARTÍCULOS EN PEDIDO",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        pedido.articulos.forEach { articulo ->
            ArticuloItem(articulo)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ESTADO DE SEGUIMIENTO
        Text(
            text = "ESTADO DE SEGUIMIENTO",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF00BFA5),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Pedido Confirmado", fontWeight = FontWeight.Bold)
                Text(text = "12 Oct. 2023", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    Toast.makeText(context, "Reportando incidencia del pedido ${pedido.id}...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Text("Reportar Problema")
            }
            Button(
                onClick = { 
                    onConfirmarRecepcion(pedido)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("Confirmar Recepción")
            }
        }
    }
}

@Composable
fun ArticuloItem(articulo: ArticuloPedido) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = articulo.nombre, fontWeight = FontWeight.Medium)
            }
            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = articulo.cantidad,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetallePedidoPreview() {
    val dummyPedido = Pedido(
        id = "2023-001",
        cliente = "Juan Pérez",
        detalles = "Suministros varios",
        total = "50.00",
        hora = "10:00 AM",
        proveedor = "Harinas del Sol",
        estado = "En Camino",
        estadoColor = Color(0xFF0D47A1),
        articulos = listOf(
            ArticuloPedido("Harina 000", "10 sacos"),
            ArticuloPedido("Levadura Seca", "2 kg")
        )
    )
    DetallePedidoContent(
        pedido = dummyPedido,
        onDismissRequest = {},
        onConfirmarRecepcion = {}
    )
}
