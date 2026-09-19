package com.developers.employee

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// ✨ NUEVOS MODELOS PARA LOS PEDIDOS REALES
data class PedidoItem(
    val nombre: String = "Producto",
    val cantidad: Int = 1,
    val precio: Double = 0.0
)

data class PedidoFirebase(
    val id: String = "",
    val userId: String = "",
    val clienteNombre: String = "Cliente",
    val direccion: String = "Sin Dirección",
    val total: Double = 0.0,
    val estado: String = "PENDIENTE",
    val fecha: Long = 0L,
    val items: List<PedidoItem> = emptyList(),
    val path: String = "" // Ruta exacta en Firebase para poder actualizarlo
)

data class LogData(
    val id: String = "", val type: String = "ENTRADA", val timestamp: Long = 0L,
    val dateFormatted: String = "", val timeFormatted: String = ""
)

class EmployeeViewModel : ViewModel() {
    var currentUserId by mutableStateOf("INVITADO")
    var userEmail by mutableStateOf("")
    var userName by mutableStateOf("Cargando...")
    var userImageUrl by mutableStateOf("") // ✨ AÑADIDO PARA FOTO DE PERFIL
    var logs by mutableStateOf<List<LogData>>(emptyList())
    var isLoading by mutableStateOf(false)

    var claveEntrada by mutableStateOf("")
    var claveSalida by mutableStateOf("")

    // ✨ LISTA EN VIVO DE PEDIDOS
    var pedidosActivos by mutableStateOf<List<PedidoFirebase>>(emptyList())

    // ✨ VARIABLES PARA REGISTRAR LOS LISTENERS Y PODER DESTRUIRLOS (STATE LEAK PREVENTION)
    private var asistenciaListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var pedidosListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun setSessionUser(uid: String, email: String) {
        if (uid != "INVITADO" && uid.isNotEmpty()) {
            currentUserId = uid; userEmail = email
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    userName = if (doc.exists()) doc.getString("nombre") ?: "Empleado" else "Empleado"
                    userImageUrl = doc.getString("imageUrl") ?: "" // ✨ CARGAMOS LA FOTO DE FIREBASE
                    fetchLogs()
                }

            // Destruir listener anterior si existía
            asistenciaListener?.remove()
            asistenciaListener = db.collection("configuracion").document("asistencia")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        claveEntrada = snapshot.getString("qrEntrada") ?: ""
                        claveSalida = snapshot.getString("qrSalida") ?: ""
                    }
                }
            // ✨ INICIAMOS LA ESCUCHA DE PEDIDOS GLOBALES
            fetchPedidosReales()
        } else {
            userName = "Empleado Invitado"
        }
    }

    private fun fetchPedidosReales() {
        val db = FirebaseFirestore.getInstance()
        
        // Destruir listener anterior si existía
        pedidosListener?.remove()
        
        // 'collectionGroup' busca en todas las subcolecciones que se llamen "pedidos"
        pedidosListener = db.collectionGroup("pedidos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        val itemsRaw = doc.get("items") as? List<HashMap<String, Any>> ?: emptyList()
                        val parsedItems = itemsRaw.map { map ->
                            PedidoItem(
                                nombre = map["nombre"]?.toString() ?: "Producto",
                                cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1,
                                precio = (map["precio"] as? Number)?.toDouble() ?: 0.0
                            )
                        }
                        val clientUserId = doc.getString("userId") ?: doc.reference.parent.parent?.id ?: ""

                        PedidoFirebase(
                            id = doc.id,
                            userId = clientUserId,
                            clienteNombre = doc.getString("clienteNombre") ?: doc.getString("cliente") ?: "Cliente Anónimo",
                            direccion = doc.getString("direccion") ?: "Recoger en sucursal",
                            total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                            estado = doc.getString("estado") ?: "PENDIENTE",
                            fecha = (doc.get("timestamp") as? Number)?.toLong() ?: (doc.get("fecha") as? Number)?.toLong() ?: 0L,
                            items = parsedItems,
                            path = doc.reference.path // Guardamos la ruta exacta para poder actualizarlo
                        )
                    }.sortedByDescending { it.fecha } // Los más recientes primero
                    pedidosActivos = lista
                }
            }
    }

    /**
     * Limpieza segura de sesión para prevenir fugas de estado y memoria
     */
    fun limpiarDatosDeSesion(onComplete: () -> Unit = {}) {
        // 1. Destruir Listeners (Prevenir que lleguen notificaciones o actualizaciones fantasma)
        asistenciaListener?.remove()
        asistenciaListener = null
        pedidosListener?.remove()
        pedidosListener = null

        // 2. Ejecutar callback para navegar al Login primero (evita parpadeos en UI)
        onComplete()

        // 3. Limpiar variables en RAM
        currentUserId = "INVITADO"
        userEmail = ""
        userName = "Cargando..."
        userImageUrl = ""
        logs = emptyList()
        claveEntrada = ""
        claveSalida = ""
        pedidosActivos = emptyList()
        
        // 4. Desconectar de Firebase Auth
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    }

    // ✨ ACTUALIZADOR DE ESTADOS EN FIREBASE MINIMALISTA
    fun actualizarEstadoPedido(pathReferencia: String, nuevoEstado: String) {
        if (pathReferencia.isEmpty()) return
        FirebaseFirestore.getInstance().document(pathReferencia).update("estado", nuevoEstado)
    }

    // (Aquí siguen intactos fetchLogs y registerAttendance...)
    fun fetchLogs() {
        if (currentUserId == "INVITADO") return
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId).collection("asistencias")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result -> logs = result.documents.mapNotNull { it.toObject(LogData::class.java) } }
    }

    fun registerAttendance(type: String, onSuccess: () -> Unit) {
        if (currentUserId == "INVITADO") return
        isLoading = true
        val logId = UUID.randomUUID().toString()
        val now = Date()
        val newLog = LogData(id = logId, type = type, timestamp = System.currentTimeMillis(), dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now), timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now))
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId).collection("asistencias").document(logId)
            .set(newLog).addOnSuccessListener { isLoading = false; fetchLogs(); onSuccess() }.addOnFailureListener { isLoading = false }
    }

    fun updateProfileImage(newUrl: String) {
        if (currentUserId == "INVITADO") return
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId)
            .update("imageUrl", newUrl)
            .addOnSuccessListener {
                userImageUrl = newUrl
            }
    }
}
