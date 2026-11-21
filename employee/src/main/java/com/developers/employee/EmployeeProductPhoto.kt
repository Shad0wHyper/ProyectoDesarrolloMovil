package com.developers.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat

@Composable
internal fun EmployeeProductPhoto(item: PedidoItem, modifier: Modifier = Modifier) {
    var image by remember(item.productoId) { mutableStateOf<String?>(null) }
    var score by remember(item.productoId) { mutableStateOf<Double?>(null) }
    DisposableEffect(item.productoId) {
        var active = true
        if (item.productoId.isNotBlank() && '/' !in item.productoId) {
            val product = FirebaseFirestore.getInstance().collection("productos").document(item.productoId)
            product.get().addOnSuccessListener { if (active) image = it.getString("imagenUrl") }
            val average = AggregateField.average("estrellas")
            product.collection("valoraciones").whereGreaterThanOrEqualTo("partida", 0)
                .aggregate(average).get(AggregateSource.SERVER)
                .addOnSuccessListener { if (active) score = it.get(average) }
        }
        onDispose { active = false }
    }

    Surface(modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            val url = image
            if (url.isNullOrBlank()) {
                Icon(Icons.Outlined.Info, contentDescription = null)
            } else {
                AsyncImage(url, item.nombre, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            score?.let { average ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(average) + "/5",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
