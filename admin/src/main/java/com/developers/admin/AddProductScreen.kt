package com.developers.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

val AdminPrimary = Color(0xFF6200EE)

// 1. Data Class IngredienteReceta
data class IngredienteReceta(
    val materiaPrimaId: String = "",
    val nombre: String = "",
    val cantidad: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    productoAEditar: Producto? = null, // Puede recibir un producto para editar
    onBack: () -> Unit,
    onSuccessSave: () -> Unit
) {
    // Relleno de campos iniciales
    var nombre by remember { mutableStateOf(productoAEditar?.nombre ?: "") }
    var precio by remember { mutableStateOf(productoAEditar?.precio ?: "") }
    var calificacion by remember { mutableStateOf(productoAEditar?.calificacion?.toString() ?: "5.0") }
    var isNuevo by remember { mutableStateOf(productoAEditar?.isNuevo ?: false) }
    var categoriaSeleccionada by remember { mutableStateOf(productoAEditar?.categoria ?: "PANES") }

    // 1. Estados para el Motor de Recetas
    var insumosDisponibles by remember { mutableStateOf<List<MateriaPrima>>(emptyList()) }
    var ingredientesAgregados by remember { mutableStateOf<List<IngredienteReceta>>(emptyList()) }
    var insumoSeleccionado by remember { mutableStateOf<MateriaPrima?>(null) }
    var cantidadInsumoText by remember { mutableStateOf("") }
    var expandedInsumoDropdown by remember { mutableStateOf(false) }

    val categoriasDisponibles = listOf("PANES", "CAFÉ", "OTROS")
    var expandedDropdown by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imageUrlGuardada = productoAEditar?.imagenUrl ?: "" // Foto guardada previamente

    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    // 2. Lectura Inicial de materia_prima y recetas existentes
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // Cargar materias primas disponibles para el Dropdown
        db.collection("materia_prima").get().addOnSuccessListener { snapshot ->
            insumosDisponibles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MateriaPrima::class.java)?.copy(id = doc.id)
            }
        }

        // Cargar receta del producto si está en modo edición
        if (productoAEditar != null) {
            db.collection("recetas").document(productoAEditar.id).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val rawList = doc.get("ingredientes") as? List<HashMap<String, Any>> ?: emptyList()
                    val parsed = rawList.map { map ->
                        IngredienteReceta(
                            materiaPrimaId = map["materiaPrimaId"]?.toString() ?: "",
                            nombre = map["nombre"]?.toString() ?: "",
                            cantidad = (map["cantidad"] as? Number)?.toInt() ?: 0
                        )
                    }
                    ingredientesAgregados = parsed
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productoAEditar != null) "Editar Producto" else "Alta de Producto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8))
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 📸 FOTO DEL PRODUCTO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                    .clickable {
                        if (!isSaving) {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(model = imageUri, contentDescription = "Vista previa", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (imageUrlGuardada.isNotEmpty()) {
                    AsyncImage(model = imageUrlGuardada, contentDescription = "Foto Guardada", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = AdminPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Toca para añadir foto del pan", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NOMBRE
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Producto") },
                leadingIcon = { Icon(Icons.Default.BakeryDining, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORÍA
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = categoriaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedDropdown = true })
                    },
                    modifier = Modifier.fillMaxWidth().clickable { expandedDropdown = true },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }, modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)) {
                    categoriasDisponibles.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat, fontWeight = FontWeight.Medium) }, onClick = { categoriaSeleccionada = cat; expandedDropdown = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRECIO Y CALIFICACIÓN
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = precio, onValueChange = { precio = it }, label = { Text("Precio ($)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = calificacion, onValueChange = { calificacion = it }, label = { Text("Estrellas (1.0 - 5.0)") },
                    leadingIcon = { Icon(Icons.Default.StarBorder, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. SECCIÓN: CONSTRUCTOR DE RECETAS (REEMPLAZO DEL STOCK MANUAL)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Receta (Insumos por unidad)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Seleccione la materia prima y la cantidad necesaria para elaborar 1 pan.", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de Materia Prima
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = insumoSeleccionado?.let { "${it.nombre} (${it.unidadMedida})" } ?: "Seleccionar Insumo...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Materia Prima") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedInsumoDropdown = true })
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedInsumoDropdown = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedInsumoDropdown,
                            onDismissRequest = { expandedInsumoDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                        ) {
                            if (insumosDisponibles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay insumos en almacén", color = Color.Gray) },
                                    onClick = { expandedInsumoDropdown = false }
                                )
                            } else {
                                insumosDisponibles.forEach { insumo ->
                                    DropdownMenuItem(
                                        text = { Text("${insumo.nombre} (${insumo.unidadMedida})", fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            insumoSeleccionado = insumo
                                            expandedInsumoDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cantidadInsumoText,
                            onValueChange = { cantidadInsumoText = it },
                            label = { Text("Cantidad (${insumoSeleccionado?.unidadMedida ?: "g"})") },
                            leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val cantInt = cantidadInsumoText.toIntOrNull() ?: 0
                                if (insumoSeleccionado != null && cantInt > 0) {
                                    val sel = insumoSeleccionado!!
                                    val nuevoIngrediente = IngredienteReceta(
                                        materiaPrimaId = sel.id,
                                        nombre = "${sel.nombre} (${sel.unidadMedida})",
                                        cantidad = cantInt
                                    )
                                    // Reemplaza o agrega el ingrediente
                                    ingredientesAgregados = ingredientesAgregados.filter { it.materiaPrimaId != sel.id } + nuevoIngrediente
                                    cantidadInsumoText = ""
                                    insumoSeleccionado = null
                                } else {
                                    Toast.makeText(context, "Seleccione un insumo e ingrese una cantidad válida", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar Insumo")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir")
                        }
                    }

                    // Lista de ingredientes agregados
                    if (ingredientesAgregados.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ingredientes de la Receta:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        ingredientesAgregados.forEach { ing ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = Color(0xFFF3E5F5),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ing.nombre}: ${ing.cantidad}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = AdminPrimary
                                    )
                                    IconButton(
                                        onClick = {
                                            ingredientesAgregados = ingredientesAgregados.filter { it.materiaPrimaId != ing.materiaPrimaId }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MARCAR COMO NUEVO
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Marcar como Nuevo", fontWeight = FontWeight.Bold)
                        Text("Aparecerá con etiqueta en la app cliente", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = isNuevo, onCheckedChange = { isNuevo = it }, colors = SwitchDefaults.colors(checkedThumbColor = AdminPrimary))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 4. BOTÓN GUARDAR (GUARDADO EN 2 PASOS: PRODUCTO Y RECETA)
            Button(
                onClick = {
                    val priceDouble = precio.toDoubleOrNull()
                    val califDouble = calificacion.toDoubleOrNull() ?: 5.0

                    if (nombre.trim().isEmpty() || priceDouble == null || (imageUri == null && imageUrlGuardada.isEmpty())) {
                        Toast.makeText(context, "Llena los datos y asegúrate de tener foto", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true

                        // Función interna para guardar todo en Firestore
                        fun guardarEnFirestore(urlFinal: String) {
                            val db = FirebaseFirestore.getInstance()
                            val itemReal = hashMapOf<String, Any>(
                                "nombre" to nombre.trim().uppercase(),
                                "precio" to priceDouble,
                                "categoria" to categoriaSeleccionada,
                                "stock" to (productoAEditar?.stock ?: 0), // 0 si es creación
                                "calificacion" to califDouble,
                                "isNuevo" to isNuevo,
                                "imagenUrl" to urlFinal
                            )

                            if (productoAEditar != null) {
                                // MODO EDICIÓN: Actualiza producto y receta
                                db.collection("productos").document(productoAEditar.id).update(itemReal)
                                    .addOnSuccessListener {
                                        val recetaMap = hashMapOf("ingredientes" to ingredientesAgregados)
                                        db.collection("recetas").document(productoAEditar.id).set(recetaMap)
                                            .addOnSuccessListener {
                                                isSaving = false
                                                Toast.makeText(context, "Producto y receta actualizados", Toast.LENGTH_SHORT).show()
                                                onSuccessSave()
                                            }
                                            .addOnFailureListener {
                                                isSaving = false
                                                Toast.makeText(context, "Producto actualizado pero falló receta", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        isSaving = false
                                        Toast.makeText(context, "Error al actualizar producto", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                // MODO CREACIÓN: Crea producto, obtiene id y guarda receta
                                db.collection("productos").add(itemReal)
                                    .addOnSuccessListener { docRef ->
                                        val nuevoId = docRef.id
                                        val recetaMap = hashMapOf("ingredientes" to ingredientesAgregados)
                                        db.collection("recetas").document(nuevoId).set(recetaMap)
                                            .addOnSuccessListener {
                                                isSaving = false
                                                Toast.makeText(context, "Producto y receta creados", Toast.LENGTH_SHORT).show()
                                                onSuccessSave()
                                            }
                                            .addOnFailureListener {
                                                isSaving = false
                                                Toast.makeText(context, "Producto creado pero falló receta", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        isSaving = false
                                        Toast.makeText(context, "Error al crear producto", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        // Lógica de imágenes
                        if (imageUri != null) {
                            val imageId = UUID.randomUUID().toString()
                            val storageRef = FirebaseStorage.getInstance().reference.child("productos/$imageId.jpg")
                            storageRef.putFile(imageUri!!)
                                .addOnSuccessListener {
                                    storageRef.downloadUrl.addOnSuccessListener { url -> guardarEnFirestore(url.toString()) }
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                    Toast.makeText(context, "Error al subir foto", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            guardarEnFirestore(imageUrlGuardada)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (productoAEditar != null) "Actualizar Producto y Receta" else "Dar de Alta Producto y Receta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
