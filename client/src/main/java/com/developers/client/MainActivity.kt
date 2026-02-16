package com.developers.client

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Interfaz visual simple (Texto en pantalla)
        val textView = TextView(this)
        textView.text = "CLIENTE: Conectando..."
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        setContentView(textView)

        // 2. Conexión a Base de Datos
        val db = FirebaseFirestore.getInstance()

        // 3. Datos únicos del CLIENTE
        val datosCliente = hashMapOf(
            "quien" to "CLIENTE APP ",
            "accion" to "Quiero comprar pan",
            "precio_visto" to 15.50,
            "fecha" to Date()
        )

        // 4. Escribir en la nube
        db.collection("validacion_final")
            .add(datosCliente)
            .addOnSuccessListener {
                val msg = "✅ CLIENTE: Éxito!\nID: ${it.id}"
                Log.d("FIREBASE_TEST", msg)
                textView.text = msg
                textView.setTextColor(android.graphics.Color.GREEN)
            }
            .addOnFailureListener {
                val msg = "❌ CLIENTE: Error\n$it"
                Log.e("FIREBASE_TEST", msg)
                textView.text = msg
                textView.setTextColor(android.graphics.Color.RED)
            }
    }
}