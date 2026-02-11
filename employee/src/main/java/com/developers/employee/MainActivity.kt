package com.developers.employee

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
        textView.text = "EMPLEADO: Conectando..."
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        setContentView(textView)

        val db = FirebaseFirestore.getInstance()

        // Datos únicos del EMPLEADO
        val datosEmpleado = hashMapOf(
            "quien" to "EMPLEADO QR ",
            "accion" to "Registrando asistencia ",
            "turno" to "Matutino",
            "fecha" to Date()
        )

        db.collection("validacion_final")
            .add(datosEmpleado)
            .addOnSuccessListener {
                val msg = "✅ EMPLEADO: Éxito!\nID: ${it.id}"
                Log.d("FIREBASE_TEST", msg)
                textView.text = msg
                textView.setTextColor(android.graphics.Color.MAGENTA) // Magenta para empleado
            }
            .addOnFailureListener {
                val msg = "❌ EMPLEADO: Error\n$it"
                Log.e("FIREBASE_TEST", msg)
                textView.text = msg
                textView.setTextColor(android.graphics.Color.RED)
            }
    }
}