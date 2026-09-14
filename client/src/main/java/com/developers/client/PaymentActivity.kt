package com.developers.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.client.ui.theme.PanAppClientTheme
import com.developers.client.ui.theme.PanAppPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar datos del intent org.chromium.intent.action.PAY
        val total = intent.getStringExtra("total") ?: "0.00"
        val merchantName = intent.getStringExtra("merchantName") ?: "PanApp Store"

        setContent {
            PanAppClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f) // Efecto de fondo para diálogo
                ) {
                    PaymentDialogUI(
                        total = total,
                        merchant = merchantName,
                        onSuccess = { method ->
                            registrarPagoFirestore(total, method)
                            enviarRespuestaExito()
                        },
                        onCancel = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun registrarPagoFirestore(total: String, metodo: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "INVITADO"
        val db = FirebaseFirestore.getInstance()

        val pago = hashMapOf(
            "total" to total,
            "metodo" to metodo,
            "fecha" to SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
            "timestamp" to System.currentTimeMillis(),
            "estado" to "COMPLETADO"
        )

        db.collection("usuarios").document(uid).collection("historial_pagos")
            .add(pago)
            .addOnSuccessListener {
                Toast.makeText(this, "Pago registrado en el historial", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enviarRespuestaExito() {
        val resultIntent = Intent()
        resultIntent.putExtra("details", "{\"status\": \"success\"}")
        resultIntent.putExtra("methodName", "https://panapp.com/pay")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

@Composable
fun PaymentDialogUI(total: String, merchant: String, onSuccess: (String) -> Unit, onCancel: () -> Unit) {
    var step by remember { mutableStateOf(1) } // 1: Resumen, 2: Tarjeta
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Payment, null, tint = PanAppPrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                
                if (step == 1) {
                    Text("Confirmar Pago", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(merchant, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a pagar:", fontWeight = FontWeight.Medium)
                        Text("$total", fontWeight = FontWeight.Bold, color = PanAppPrimary, fontSize = 20.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                    ) {
                        Text("Pagar con Tarjeta")
                    }
                    
                    TextButton(onClick = onCancel) {
                        Text("Cancelar", color = Color.Gray)
                    }
                } else {
                    Text("Detalles de Tarjeta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = cardNumber, 
                        onValueChange = { if (it.length <= 16) cardNumber = it }, 
                        label = { Text("Número de Tarjeta") }, 
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("XXXX XXXX XXXX XXXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expiry, 
                            onValueChange = { if (it.length <= 5) expiry = it }, 
                            label = { Text("MM/YY") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = cvc, 
                            onValueChange = { if (it.length <= 3) cvc = it }, 
                            label = { Text("CVC") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { onSuccess("TARJETA") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = cardNumber.length >= 15 && cvc.length >= 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                    ) {
                        Text("Confirmar $total")
                    }
                    
                    TextButton(onClick = { step = 1 }) {
                        Text("Volver", color = Color.Gray)
                    }
                }
            }
        }
    }
}
