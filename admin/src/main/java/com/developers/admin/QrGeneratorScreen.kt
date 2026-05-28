package com.developers.admin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.util.UUID

// --- COLORES CORPORATIVOS PARA QR ---
val ColorEntrada = Color(0xFF4CAF50) // Verde
val ColorSalida = Color(0xFFF44336) // Rojo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(onBack: () -> Unit) {
    var qrEntrada by remember { mutableStateOf("Cargando...") }
    var qrSalida by remember { mutableStateOf("Cargando...") }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Estado para el popup de QR ampliado
    var mostrarQrGrande by remember { mutableStateOf(false) }
    var qrAmpliadoContenido by remember { mutableStateOf<Pair<String, Color>?>(null) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("configuracion").document("asistencia").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    qrEntrada = doc.getString("qrEntrada") ?: "PANAPP_ENT_0000"
                    qrSalida = doc.getString("qrSalida") ?: "PANAPP_SAL_0000"
                } else {
                    qrEntrada = "PANAPP_ENT_${UUID.randomUUID().toString().take(4).uppercase()}"
                    qrSalida = "PANAPP_SAL_${UUID.randomUUID().toString().take(4).uppercase()}"
                }
            }
    }

    fun generarNuevasClaves() {
        isSaving = true
        val nuevaEntrada = "PANAPP_ENT_${UUID.randomUUID().toString().take(4).uppercase()}"
        val nuevaSalida = "PANAPP_SAL_${UUID.randomUUID().toString().take(4).uppercase()}"
        val db = FirebaseFirestore.getInstance()
        db.collection("configuracion").document("asistencia")
            .set(mapOf("qrEntrada" to nuevaEntrada, "qrSalida" to nuevaSalida))
            .addOnSuccessListener {
                qrEntrada = nuevaEntrada
                qrSalida = nuevaSalida
                isSaving = false
                Toast.makeText(context, "Códigos QR actualizados y sincronizados", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                isSaving = false
                Toast.makeText(context, "Error al guardar en Firebase", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generador QR de Asistencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F8F8)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Muestra estos códigos en la sucursal para que los empleados escaneen su entrada o salida.", textAlign = TextAlign.Center, color = Color.Gray)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QrCardView(modifier = Modifier.weight(1f), title = "Código de ENTRADA", color = ColorEntrada, qrContent = qrEntrada, onClick = { qrAmpliadoContenido = Pair(qrEntrada, ColorEntrada); mostrarQrGrande = true })
                QrCardView(modifier = Modifier.weight(1f), title = "Código de SALIDA", color = ColorSalida, qrContent = qrSalida, onClick = { qrAmpliadoContenido = Pair(qrSalida, ColorSalida); mostrarQrGrande = true })
            }

            Button(onClick = { generarNuevasClaves() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), enabled = !isSaving, colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)) {
                if (isSaving) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) } else { Icon(Icons.Default.Refresh, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Generar Nuevos Códigos QR Hoy", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }

    // --- POPUP DE QR GRANDE ---
    if (mostrarQrGrande && qrAmpliadoContenido != null) {
        val (contenido, color) = qrAmpliadoContenido!!
        val qrBitmapGrande = generarQrConLogo(LocalContext.current, contenido, color)

        AlertDialog(
            onDismissRequest = { mostrarQrGrande = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("QR de Asistencia", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { mostrarQrGrande = false }, modifier = Modifier.background(Color(0xFFEEEEEE), CircleShape)) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray) }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (qrBitmapGrande != null) {
                        Image(bitmap = qrBitmapGrande.asImageBitmap(), contentDescription = "QR Grande", modifier = Modifier.size(300.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp))
                    } else {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(200.dp), tint = Color.LightGray)
                    }
                    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(contenido, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (qrBitmapGrande != null) {
                            val filename = "${if (color == ColorEntrada) "Entrada" else "Salida"}_$contenido"
                            guardarQrEnGaleria(context, qrBitmapGrande, filename)
                            mostrarQrGrande = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Guardar como Imagen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
fun QrCardView(modifier: Modifier, title: String, color: Color, qrContent: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val qrBitmap = generarQrConLogo(context, qrContent, color)
    Card(modifier = modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (qrBitmap != null) {
                Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp))
            } else {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                Text(qrContent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// --- ✨ FUNCIÓN OPTIMIZADA: GENERADOR DE QR INCRUSTANDO LOGO PNG DIRECTAMENTE ---
private fun generarQrConLogo(context: android.content.Context, content: String, color: Color): Bitmap? {
    return try {
        val size = 512
        val hints = hashMapOf<EncodeHintType, Any>(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H)
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val colorInt = android.graphics.Color.parseColor(String.format("#%06X", 0xFFFFFF and color.value.toInt()))

        for (x in 0 until size) {
            for (y in 0 until size) { bmp.setPixel(x, y, if (bitMatrix[x, y]) colorInt else android.graphics.Color.WHITE) }
        }

        // Pega directamente tu imagen .png con transparencias o bordes redondos nativos
        val logoOriginal = BitmapFactory.decodeResource(context.resources, R.drawable.log)
        if (logoOriginal != null) {
            val logoSize = (size * 0.25).toInt()
            val scaledLogo = Bitmap.createScaledBitmap(logoOriginal, logoSize, logoSize, false)
            Canvas(bmp).drawBitmap(scaledLogo, ((size - logoSize) / 2).toFloat(), ((size - logoSize) / 2).toFloat(), null)
        }
        bmp
    } catch (e: Exception) { null }
}

private fun guardarQrEnGaleria(context: android.content.Context, bitmap: Bitmap, filename: String) {
    val contextResolver = context.contentResolver
    val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val imageDetails = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "PanappQRs")
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val imageUri = contextResolver.insert(imageCollection, imageDetails)
    if (imageUri != null) {
        try {
            contextResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                contextResolver.update(imageUri, imageDetails, null, null)
            }
            Toast.makeText(context, "QR Guardado en Galería -> Imágenes/PanappQRs", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            contextResolver.delete(imageUri, null, null)
            Toast.makeText(context, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Error creando Uri de imagen", Toast.LENGTH_SHORT).show()
    }
}