package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduccionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var productosList by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var cantidadProducidaText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    // Estados para gestión de errores de falta de stock
    var showFaltaStockDialog by remember { mutableStateOf(false) }
    var faltantesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var insumoUnicoNombre by remember { mutableStateOf("") }

    // 1. Lectura de productos desde Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("productos").get().addOnSuccessListener { snapshot ->
            productosList = snapshot.documents.mapNotNull { doc ->
                val stockReal = doc.getLong("stock")?.toInt() ?: 0
                val statusL = when {
                    stockReal == 0 -> "AGOTADO"
                    stockReal < 5 -> "Crítico"
                    else -> "Óptimo"
                }
                val statusC = when {
                    stockReal == 0 -> Color.Red
                    stockReal < 5 -> Color(0xFFF44336)
                    else -> Color(0xFF4CAF50)
                }

                Producto(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "Sin nombre",
                    categoria = doc.getString("categoria") ?: "Otros",
                    stock = stockReal,
                    statusLabel = statusL,
                    statusColor = statusC,
                    precio = String.format("%.2f", doc.getDouble("precio") ?: 0.0),
                    imagenUrl = doc.getString("imagenUrl") ?: "",
                    calificacion = doc.getDouble("calificacion") ?: 5.0,
                    isNuevo = doc.getBoolean("isNuevo") ?: false
                )
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    // 2. Lógica de Validación Previa y Registro Batched Write
    fun procesarRegistroProduccion() {
        val cantidadProducida = cantidadProducidaText.toIntOrNull() ?: 0
        if (productoSeleccionado == null || cantidadProducida <= 0) {
            Toast.makeText(context, "Seleccione un producto e ingrese una cantidad mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        val producto = productoSeleccionado!!
        isProcessing = true
        val db = FirebaseFirestore.getInstance()

        // Paso A: Consultar la receta del producto
        db.collection("recetas").document(producto.id).get().addOnSuccessListener { recetaDoc ->
            if (!recetaDoc.exists()) {
                isProcessing = false
                Toast.makeText(context, "Este producto no tiene receta registrada. Registre su receta en Almacén.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val rawList = recetaDoc.get("ingredientes") as? List<HashMap<String, Any>> ?: emptyList()
            val ingredientes = rawList.map { map ->
                IngredienteReceta(
                    materiaPrimaId = map["materiaPrimaId"]?.toString() ?: "",
                    nombre = map["nombre"]?.toString() ?: "",
                    cantidad = (map["cantidad"] as? Number)?.toInt() ?: 0
                )
            }

            if (ingredientes.isEmpty()) {
                isProcessing = false
                Toast.makeText(context, "La receta de este producto no contiene ingredientes.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            // Paso B: Consultar inventario actual de materia_prima
            db.collection("materia_prima").get().addOnSuccessListener { materiaPrimaSnapshot ->
                val materiasPrimas = materiaPrimaSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(MateriaPrima::class.java)?.copy(id = doc.id)
                }

                val listaFaltantesTemp = mutableListOf<String>()
                var unicoInsumoNombre = ""

                // Paso C: Comparar inventarios
                for (ing in ingredientes) {
                    val requerido = ing.cantidad * cantidadProducida
                    val insumoEncontrado = materiasPrimas.find { it.id == ing.materiaPrimaId }

                    val cantidadDisponible = insumoEncontrado?.cantidadActual ?: 0.0
                    val nombreInsumo = insumoEncontrado?.nombre ?: ing.nombre
                    val unidad = insumoEncontrado?.unidadMedida ?: "g"

                    if (insumoEncontrado == null || requerido > cantidadDisponible) {
                        unicoInsumoNombre = nombreInsumo
                        val lineaError = "- $nombreInsumo: Necesaria ${requerido}${unidad} / Disponible ${cantidadDisponible}${unidad}"
                        listaFaltantesTemp.add(lineaError)
                    }
                }

                // Paso D: Bifurcación (Éxito vs Fracaso)
                if (listaFaltantesTemp.isNotEmpty()) {
                    // SI HAY FALTANTES: Detener y mostrar Dialog
                    isProcessing = false
                    faltantesList = listaFaltantesTemp
                    insumoUnicoNombre = unicoInsumoNombre
                    showFaltaStockDialog = true
                } else {
                    // SI HAY STOCK SUFICIENTE: Batched Write
                    val batch = db.batch()

                    // 1. Sumar producción al stock del producto
                    val productoRef = db.collection("productos").document(producto.id)
                    batch.update(productoRef, "stock", FieldValue.increment(cantidadProducida.toLong()))

                    // 2. Restar ingredientes consumidos de materia_prima
                    for (ing in ingredientes) {
                        val requerido = ing.cantidad * cantidadProducida
                        val insumoRef = db.collection("materia_prima").document(ing.materiaPrimaId)
                        batch.update(insumoRef, "cantidadActual", FieldValue.increment(-requerido.toDouble()))
                    }

                    // 3. Ejecutar la transacción en la nube
                    batch.commit().addOnSuccessListener {
                        isProcessing = false
                        Toast.makeText(context, "¡Producción de ${cantidadProducida} unidades de ${producto.nombre} registrada!", Toast.LENGTH_LONG).show()
                        onBack()
                    }.addOnFailureListener { e ->
                        isProcessing = false
                        Toast.makeText(context, "Error en la transacción de producción: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                isProcessing = false
                Toast.makeText(context, "Error al consultar inventario de materia prima", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            isProcessing = false
            Toast.makeText(context, "Error al consultar la receta del producto", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de Producción", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Encabezado Ilustrativo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF3F51B5), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Factory, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Horneados Diarios (ERP)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                        Text("Registra la hornada. El sistema descontará los insumos de materia prima automáticamente.", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF3F51B5), modifier = Modifier.padding(32.dp))
            } else {
                // Selector de Producto
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = productoSeleccionado?.nombre ?: "Seleccionar Producto...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto Elaborado") },
                        leadingIcon = { Icon(Icons.Default.BakeryDining, contentDescription = null) },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedDropdown = true })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White)
                    ) {
                        if (productosList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay productos en catálogo", color = Color.Gray) },
                                onClick = { expandedDropdown = false }
                            )
                        } else {
                            productosList.forEach { producto ->
                                DropdownMenuItem(
                                    text = { Text("${producto.nombre} (Stock actual: ${producto.stock})", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        productoSeleccionado = producto
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Cantidad Producida
                OutlinedTextField(
                    value = cantidadProducidaText,
                    onValueChange = { cantidadProducidaText = it },
                    label = { Text("Cantidad de Panes Horneados (Piezas)") },
                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón Confirmar Registro
                Button(
                    onClick = { procesarRegistroProduccion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar y Descontar Insumos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. UI DEL DIALOG DE ERROR DINÁMICO
    if (showFaltaStockDialog) {
        val tituloDinamico = if (faltantesList.size == 1) {
            "Cantidad de $insumoUnicoNombre insuficiente"
        } else {
            "Ingredientes insuficientes"
        }

        AlertDialog(
            onDismissRequest = { showFaltaStockDialog = false },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(40.dp)) },
            title = { Text(tituloDinamico, fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 16.sp) },
            text = {
                Column {
                    Text("No hay inventario suficiente en almacén para hornear esta cantidad:", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faltantesList.joinToString("\n\n"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFaltaStockDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Entendido", color = Color.White)
                }
            },
            containerColor = Color.White
        )
    }
}
