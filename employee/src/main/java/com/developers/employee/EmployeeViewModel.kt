package com.developers.employee

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// ✨ NUEVOS MODELOS PARA LOS PEDIDOS REALES
data class PedidoItem(
    val productoId: String = "",
    val nombre: String = localizedText(TextR.string.pan_product),
    val cantidad: Int = 1,
    val precio: Double = 0.0
)

data class PedidoFirebase(
    val id: String = "",
    val userId: String = "",
    val clienteNombre: String = localizedText(TextR.string.pan_customer),
    val direccion: String = localizedText(TextR.string.pan_no_address),
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
    // Resolve local fallback text when read, so retained ViewModels follow locale changes.
    private var userNameResource by mutableStateOf<Int?>(TextR.string.pan_loading)
    private var userNameValue by mutableStateOf("")
    var userName: String
        get() = userNameResource?.let { localizedText(it) } ?: userNameValue
        set(value) {
            userNameResource = null
            userNameValue = value
        }
    var userImageUrl by mutableStateOf("") // ✨ AÑADIDO PARA FOTO DE PERFIL
    var logs by mutableStateOf<List<LogData>>(emptyList())
    var isLoading by mutableStateOf(false)

    var claveEntrada by mutableStateOf("")
    var claveSalida by mutableStateOf("")

    // ✨ LISTA EN VIVO DE PEDIDOS
    var pedidosActivos by mutableStateOf<List<PedidoFirebase>>(emptyList())
    var pedidosError by mutableStateOf(false)

    // ✨ VARIABLES PARA REGISTRAR LOS LISTENERS Y PODER DESTRUIRLOS (STATE LEAK PREVENTION)
    private var asistenciaListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var pedidosListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun setSessionUser(uid: String, email: String) {
        if (uid != "INVITADO" && uid.isNotEmpty()) {
            currentUserId = uid; userEmail = email
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val storedName = if (doc.exists()) doc.getString("nombre") else null
                    if (storedName == null) userNameResource = TextR.string.pan_employee
                    else userName = storedName
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
            userNameResource = TextR.string.pan_guest_employee
        }
    }

    private fun fetchPedidosReales() {
        val db = FirebaseFirestore.getInstance()

        // Destruir listener anterior si existía
        pedidosListener?.remove()
        pedidosError = false

        // 'collectionGroup' busca en todas las subcolecciones que se llamen "pedidos"
        pedidosListener = db.collectionGroup("pedidos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EmployeeViewModel", "collectionGroup pedidos failed", error)
                    fetchPedidosFallback(db)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    pedidosError = false
                    pedidosActivos = snapshot.documents
                        .mapNotNull(::parsePedidoDocument)
                        .distinctBy { it.path.ifEmpty { it.id } }
                        .sortedByDescending { it.fecha } // Los más recientes primero
                }
            }
    }

    private fun fetchPedidosFallback(db: FirebaseFirestore) {
        val collected = linkedMapOf<String, PedidoFirebase>()
        var rootDone = false
        var usersDone = false
        var anySuccess = false

        fun finishCheck() {
            if (!rootDone || !usersDone) return
            if (anySuccess) {
                pedidosError = false
                pedidosActivos = collected.values.sortedByDescending { it.fecha }
            } else {
                pedidosError = true
            }
        }

        db.collection("pedidos").get()
            .addOnSuccessListener { snap ->
                anySuccess = true
                snap.documents.mapNotNull(::parsePedidoDocument).forEach { pedido ->
                    collected[pedido.path.ifEmpty { pedido.id }] = pedido
                }
                rootDone = true
                finishCheck()
            }
            .addOnFailureListener { e ->
                Log.e("EmployeeViewModel", "Fallback root pedidos failed", e)
                rootDone = true
                finishCheck()
            }

        db.collection("usuarios").get()
            .addOnSuccessListener { userSnap ->
                val users = userSnap.documents
                if (users.isEmpty()) {
                    anySuccess = true
                    usersDone = true
                    finishCheck()
                    return@addOnSuccessListener
                }
                var pending = users.size
                for (userDoc in users) {
                    db.collection("usuarios").document(userDoc.id).collection("pedidos").get()
                        .addOnSuccessListener { orderSnap ->
                            anySuccess = true
                            orderSnap.documents.mapNotNull(::parsePedidoDocument).forEach { pedido ->
                                collected[pedido.path.ifEmpty { pedido.id }] = pedido
                            }
                            pending--
                            if (pending == 0) {
                                usersDone = true
                                finishCheck()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("EmployeeViewModel", "Fallback user ${userDoc.id} pedidos failed", e)
                            pending--
                            if (pending == 0) {
                                usersDone = true
                                finishCheck()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("EmployeeViewModel", "Fallback usuarios query failed", e)
                usersDone = true
                finishCheck()
            }
    }

    private fun parsePedidoDocument(doc: DocumentSnapshot): PedidoFirebase? = runCatching {
        val itemsRaw = (doc.get("items") as? List<*>)?.mapNotNull { it as? Map<*, *> }.orEmpty()
        val parsedItems = itemsRaw.map { map ->
            PedidoItem(
                productoId = map["productoId"] as? String ?: "",
                nombre = map["nombre"]?.toString() ?: localizedText(TextR.string.pan_product),
                cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1,
                precio = (map["precio"] as? Number)?.toDouble() ?: 0.0
            )
        }
        val clientUserId = (doc.get("userId") as? String)?.takeIf { it.isNotBlank() }
            ?: doc.reference.parent.parent?.id
            ?: ""

        val clienteNombre = (doc.get("clienteNombre") as? String)?.takeIf { it.isNotBlank() }
            ?: (doc.get("cliente") as? String)?.takeIf { it.isNotBlank() }
            ?: localizedText(TextR.string.pan_anonymous_customer)

        val direccion = (doc.get("direccion") as? String)?.takeIf { it.isNotBlank() }
            ?: (doc.get("direccionEnvio") as? String)?.takeIf { it.isNotBlank() }
            ?: localizedText(TextR.string.pan_pickup)

        PedidoFirebase(
            id = doc.id,
            userId = clientUserId,
            clienteNombre = clienteNombre,
            direccion = direccion,
            total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
            estado = normalizeOrderStatus(doc.get("estado") as? String, doc.get("status") as? String),
            fecha = parseOrderTimestampMillis(doc.get("timestamp"), doc.get("fecha")),
            items = parsedItems,
            path = doc.reference.path // Guardamos la ruta exacta para poder actualizarlo
        )
    }.onFailure {
        Log.e("EmployeeViewModel", "Error parsing pedido ${doc.reference.path}", it)
    }.getOrNull()

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
        userNameResource = TextR.string.pan_loading
        userImageUrl = ""
        logs = emptyList()
        claveEntrada = ""
        claveSalida = ""
        pedidosActivos = emptyList()
        pedidosError = false

        // 4. Desconectar de Firebase Auth
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    }

    // ✨ ACTUALIZADOR DE ESTADOS EN FIREBASE MINIMALISTA
    fun actualizarEstadoPedido(pathReferencia: String, nuevoEstado: String) {
        if (pathReferencia.isEmpty()) return
        val canonical = normalizeOrderStatus(nuevoEstado, null)
        FirebaseFirestore.getInstance().document(pathReferencia)
            .update(mapOf("estado" to canonical, "status" to canonical))
            .addOnFailureListener { e ->
                Log.e("EmployeeViewModel", "Error updating pedido $pathReferencia", e)
            }
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
        val nowMillis = System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now)
        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val newLogMap = hashMapOf<String, Any>(
            "id" to logId,
            "type" to type,
            "timestamp" to nowMillis,
            "dateFormatted" to dateFormatted,
            "timeFormatted" to timeFormatted,
            "userId" to currentUserId,
            "employeeName" to userName
        )
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId).collection("asistencias").document(logId)
            .set(newLogMap).addOnSuccessListener { isLoading = false; fetchLogs(); onSuccess() }.addOnFailureListener { isLoading = false }
    }

    fun updateProfileImage(newUrl: String) {
        if (currentUserId == "INVITADO") return
        FirebaseFirestore.getInstance().collection("usuarios").document(currentUserId)
            .update("imageUrl", newUrl)
            .addOnSuccessListener {
                userImageUrl = newUrl
            }
    }

    override fun onCleared() {
        super.onCleared()
        asistenciaListener?.remove()
        asistenciaListener = null
        pedidosListener?.remove()
        pedidosListener = null
    }
}
