package com.developers.employee

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class LogData(
    val id: String = "",
    val type: String = "ENTRADA",
    val timestamp: Long = 0L,
    val dateFormatted: String = "",
    val timeFormatted: String = ""
)

class EmployeeViewModel : ViewModel() {
    var currentUserId by mutableStateOf("INVITADO")
    var userEmail by mutableStateOf("")
    var userName by mutableStateOf("Cargando...")
    var logs by mutableStateOf<List<LogData>>(emptyList())
    var isLoading by mutableStateOf(false)

    // ✨ VARIABLES DINÁMICAS
    var claveEntrada by mutableStateOf("")
    var claveSalida by mutableStateOf("")

    fun setSessionUser(uid: String, email: String) {
        if (uid != "INVITADO" && uid.isNotEmpty()) {
            currentUserId = uid
            userEmail = email
            val db = FirebaseFirestore.getInstance()

            // Traer nombre y logs
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    userName = if (doc.exists()) doc.getString("nombre") ?: "Empleado" else "Empleado"
                    fetchLogs()
                }

            // ✨ LECTURA DINÁMICA DE LOS QR DESDE FIREBASE
            db.collection("configuracion").document("asistencia")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        claveEntrada = snapshot.getString("qrEntrada") ?: ""
                        claveSalida = snapshot.getString("qrSalida") ?: ""
                    }
                }
        } else {
            userName = "Empleado Invitado"
        }
    }

    fun fetchLogs() {
        if (currentUserId == "INVITADO") return
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId).collection("asistencias")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                logs = result.documents.mapNotNull { it.toObject(LogData::class.java) }
            }
    }

    fun registerAttendance(type: String, onSuccess: () -> Unit) {
        if (currentUserId == "INVITADO") return
        isLoading = true
        val logId = UUID.randomUUID().toString()
        val now = Date()
        val newLog = LogData(
            id = logId, type = type, timestamp = System.currentTimeMillis(),
            dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now),
            timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        )
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId).collection("asistencias").document(logId)
            .set(newLog)
            .addOnSuccessListener {
                isLoading = false
                fetchLogs()
                onSuccess()
            }.addOnFailureListener { isLoading = false }
    }
}