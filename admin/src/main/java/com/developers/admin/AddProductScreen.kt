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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    onSuccessSave: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    // ✨ NUEVOS CAMPOS: Stock y Calificación
    var stockInicial by remember { mutableStateOf("") }
    var calificacion by remember { mutableStateOf("5.0") }
    var isNuevo by remember { mutableStateOf(false) }

    val categoriasDisponibles = listOf("PANES", "CAFÉ", "OTROS")
    var categoriaSeleccionada by remember { mutableStateOf("PANES") }
    var expandedDropdown by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alta de Producto", fontWeight = FontWeight.Bold) },
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

            // 📸 FOTO
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
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Vista previa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
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
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                ) {
                    categoriasDisponibles.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, fontWeight = FontWeight.Medium) },
                            onClick = {
                                categoriaSeleccionada = cat
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRECIO Y STOCK (En una sola fila)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio ($)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stockInicial,
                    onValueChange = { stockInicial = it },
                    label = { Text("Stock Inicial") },
                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CALIFICACIÓN ESTRELLAS
            OutlinedTextField(
                value = calificacion,
                onValueChange = { calificacion = it },
                label = { Text("Calificación Estrellas (1.0 - 5.0)") },
                leadingIcon = { Icon(Icons.Default.StarBorder, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ES NUEVO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Marcar como Nuevo", fontWeight = FontWeight.Bold)
                        Text("Aparecerá con etiqueta en la app cliente", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isNuevo,
                        onCheckedChange = { isNuevo = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AdminPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN GUARDAR EN FIREBASE
            Button(
                onClick = {
                    val priceDouble = precio.toDoubleOrNull()
                    val stockInt = stockInicial.toIntOrNull() ?: 0
                    val calificacionDouble = calificacion.toDoubleOrNull() ?: 5.0

                    if (nombre.trim().isEmpty() || priceDouble == null || imageUri == null) {
                        Toast.makeText(context, "Llena todos los campos principales y la foto", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true
                        val imageId = UUID.randomUUID().toString()
                        val storageRef = FirebaseStorage.getInstance().reference.child("productos/$imageId.jpg")

                        storageRef.putFile(imageUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    val db = FirebaseFirestore.getInstance()
                                    // ✨ Objeto completo que se manda a Clientes y Administradores
                                    val itemReal = hashMapOf(
                                        "nombre" to nombre.trim().uppercase(),
                                        "precio" to priceDouble,
                                        "categoria" to categoriaSeleccionada,
                                        "stock" to stockInt,
                                        "calificacion" to calificacionDouble,
                                        "isNuevo" to isNuevo,
                                        "imagenUrl" to downloadUrl.toString()
                                    )
                                    db.collection("productos").add(itemReal)
                                        .addOnSuccessListener {
                                            isSaving = false
                                            Toast.makeText(context, "Producto guardado", Toast.LENGTH_SHORT).show()
                                            onSuccessSave()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Error al subir foto", Toast.LENGTH_SHORT).show()
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
                    Text("Dar de Alta Producto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}