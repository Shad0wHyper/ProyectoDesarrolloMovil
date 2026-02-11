package com.developers.admin // <--- OJO: El paquete cambia aquí

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

        val textView = TextView(this)
        textView.text = "ADMIN: Conectando..."
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        setContentView(textView)

        val db = FirebaseFirestore.getInstance()

        // Datos únicos del ADMIN
        val datosAdmin = hashMapOf(
            "quien" to "ADMINISTRADOR ",
            "accion" to "Actualizando inventario ",
            "stock_nuevo" to 500,
            "fecha" to Date()
        )

        db.collection("validacion_final")
            .add(datosAdmin)
            .addOnSuccessListener {
                val msg = "✅ ADMIN: Éxito!\nID: ${it.id}"
                Log.d("FIREBASE_TEST", msg)
                textView.text = msg
                textView.setTextColor(android.graphics.Color.BLUE) // Azul para admin
            }
            .addOnFailureListener {
                val msg = "❌ ADMIN: Error\n$it"
                Log.e("FIREBASE_TEST", msg)
                textView.text = msg
                textView.setTextColor(android.graphics.Color.RED)
            }
    }
}