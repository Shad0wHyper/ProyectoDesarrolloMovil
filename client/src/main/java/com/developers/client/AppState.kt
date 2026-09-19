package com.developers.client

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

// ✨ NUEVO: Modelo para leer los artículos detallados desde Firebase
data class OrderItemDetail(
    val nombre: String = "",
    val cantidad: Int = 1,
    val precio: Double = 0.0
)

data class CartItem(
    val id: String,
    val name: String,
    val desc: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int
)

data class OrderData(
    val id: String = "",
    val userId: String = "",
    val status: String = "PENDIENTE",
    val date: String = "",
    val mainItem: String = "",
    val itemCount: Int = 0,
    val total: String = "",
    val timestamp: Long = 0L,
    val direccionEnvio: String = "",
    val metodoPago: String = "Tarjeta",
    val pagado: Boolean = false,
    val itemsList: List<OrderItemDetail> = emptyList() // ✨ AQUÍ SE GUARDAN TODOS LOS ARTÍCULOS
)

class AppViewModel : ViewModel() {
    var isDarkMode by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)

    // Datos del Usuario
    var currentUserId by mutableStateOf("INVITADO")
    var userName by mutableStateOf("Cargando...")
    var userEmail by mutableStateOf("Cargando...")
    var userPhone by mutableStateOf("...")
    var userImageUrl by mutableStateOf("")
    var userAddress by mutableStateOf("")
    var userAddressesList by mutableStateOf<List<String>>(emptyList())

    // Datos de Banco / Saldo
    var userBalance by mutableStateOf(0.0)
    var isFetchingBalance by mutableStateOf(false)

    // Datos de Pago Guardados
    var userCardLast4 by mutableStateOf("")
    var userCardExp by mutableStateOf("")
    var hasSavedCard by mutableStateOf(false)

    // Variable para almacenar el Listener de Firestore y destruirlo al salir
    private var ordersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // Resto del código...
    var tempCardNumber by mutableStateOf("")
    var tempExpiryDate by mutableStateOf("")
    var tempCVC by mutableStateOf("")
    var tempSaveCard by mutableStateOf(false)

    fun resetTempPaymentData() {
        tempCardNumber = ""
        tempExpiryDate = userCardExp
        tempCVC = ""
        tempSaveCard = false
    }

    var cartItems by mutableStateOf<List<CartItem>>(emptyList())
    var ordersList by mutableStateOf<List<OrderData>>(emptyList())

    val deliveryFee = 3.50
    val taxRate = 0.08

    val cartUniqueItems: Int get() = cartItems.size
    val cartTotalQuantity: Int get() = cartItems.sumOf { it.quantity }

    val cartSubtotal: Double get() = cartItems.sumOf { it.price * it.quantity }
    val taxes: Double get() = cartSubtotal * taxRate
    val total: Double get() = if (cartItems.isEmpty()) 0.0 else cartSubtotal + taxes + deliveryFee

    fun setSessionUser(uid: String, email: String) {
        if (uid == "INVITADO" || uid.isEmpty()) {
            currentUserId = "INVITADO"
            userName = "Invitado"
            userEmail = "Inicia sesión para ordenar"
            return
        }
        currentUserId = uid
        userEmail = email

        // Obtener y guardar token FCM para notificaciones
        registrarTokenFCM()

        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userName = doc.getString("nombre") ?: "Usuario"
                    userPhone = doc.getString("telefono") ?: "..."
                    userImageUrl = doc.getString("imageUrl") ?: ""
                    userAddress = doc.getString("direccion") ?: ""
                    userAddressesList = doc.get("listaDirecciones") as? List<String> ?: emptyList()
                    
                    // Cargar saldo del banco
                    userBalance = doc.getDouble("saldo") ?: 1500.0 // Saldo inicial de cortesía si no existe
                    if (!doc.contains("saldo")) {
                        db.collection("usuarios").document(uid).update("saldo", userBalance)
                    }

                    // Cargar datos de tarjeta
                    userCardLast4 = doc.getString("cardLast4") ?: ""
                    userCardExp = doc.getString("cardExp") ?: ""
                    hasSavedCard = userCardLast4.isNotEmpty()
                } else {
                    userName = "PanApp User"
                }
            }

        // ESCUCHA EN VIVO DE PEDIDOS
        // Desconectamos cualquier listener anterior antes de crear uno nuevo
        ordersListenerRegistration?.remove()
        
        ordersListenerRegistration = db.collection("usuarios").document(uid).collection("pedidos")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { d ->
                        val ts = d.getLong("timestamp") ?: 0L
                        val dateStr = if (ts > 0) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts)) else d.getString("date") ?: ""

                        val estadoActual = d.getString("estado") ?: d.getString("status") ?: "PENDIENTE"

                        // ✨ PARSEO COMPLETO DE LOS ARTÍCULOS
                        val itemsRaw = d.get("items") as? List<HashMap<String, Any>> ?: emptyList()
                        val parsedItems = itemsRaw.map { map ->
                            OrderItemDetail(
                                nombre = map["nombre"]?.toString() ?: "Producto",
                                cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1,
                                precio = (map["precio"] as? Number)?.toDouble() ?: 0.0
                            )
                        }

                        OrderData(
                            id = d.id,
                            userId = d.getString("userId") ?: uid,
                            status = estadoActual.uppercase(),
                            date = dateStr,
                            mainItem = d.getString("mainItem") ?: "Pedido PanApp",
                            itemCount = d.getLong("itemCount")?.toInt() ?: 1,
                            total = String.format("$%.2f", d.getDouble("total") ?: 0.0),
                            timestamp = ts,
                            direccionEnvio = d.getString("direccion") ?: d.getString("direccionEnvio") ?: "",
                            metodoPago = d.getString("metodoPago") ?: "Tarjeta",
                            pagado = d.getBoolean("pagado") ?: false,
                            itemsList = parsedItems // ✨ SE LO PASAMOS A LA TARJETA
                        )
                    }.sortedByDescending { it.timestamp }
                    ordersList = list
                }
            }
    }

    fun updateProfileData(name: String, phone: String, address: String, onComplete: () -> Unit) {
        val uid = currentUserId
        if (uid == "INVITADO") return
        
        val newList = if (!userAddressesList.contains(address) && address.isNotEmpty()) {
            userAddressesList + address
        } else userAddressesList

        val updates = mapOf(
            "nombre" to name, 
            "telefono" to phone, 
            "direccion" to address,
            "listaDirecciones" to newList
        )
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).update(updates)
            .addOnSuccessListener {
                userName = name; userPhone = phone; userAddress = address
                userAddressesList = newList
                onComplete()
            }
    }

    fun addAddress(address: String) {
        val uid = currentUserId
        if (uid == "INVITADO" || address.isEmpty()) return
        if (userAddressesList.contains(address)) {
            userAddress = address
            return
        }
        val newList = userAddressesList + address
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .update(mapOf("listaDirecciones" to newList, "direccion" to address))
            .addOnSuccessListener {
                userAddressesList = newList
                userAddress = address
            }
    }

    fun savePaymentCard(last4: String, exp: String) {
        val uid = currentUserId
        if (uid == "INVITADO") return
        val updates = mapOf("cardLast4" to last4, "cardExp" to exp)
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).update(updates)
            .addOnSuccessListener {
                userCardLast4 = last4
                userCardExp = exp
                hasSavedCard = true
            }
    }

    /**
     * Crea un PaymentIntent real con Stripe para procesar un pago.
     * En producción, esto se debe llamar a tu Servidor Backend.
     */
    fun createStripePaymentIntent(amount: Double, methodType: String = "card", onResult: (String?) -> Unit) {
        val client = OkHttpClient()
        
        // El monto debe estar en centavos para Stripe (ej: $10.00 -> 1000)
        val amountInCents = (amount * 100).toInt()
        
        val bodyBuilder = FormBody.Builder()
            .add("amount", amountInCents.toString())
            .add("currency", "mxn")
        
        if (methodType == "oxxo") {
            bodyBuilder.add("payment_method_types[]", "oxxo")
        } else {
            bodyBuilder.add("payment_method_types[]", "card")
        }

        val request = Request.Builder()
            .url("https://api.stripe.com/v1/payment_intents")
            .addHeader("Authorization", "Bearer ${PaymentConfig.SECRET_KEY}")
            .post(bodyBuilder.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Stripe", "Error creando PaymentIntent", e)
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val clientSecret = json.getString("client_secret")
                    onResult(clientSecret)
                } else {
                    Log.e("Stripe", "Error en respuesta Stripe: ${response.code} - $body")
                    onResult(null)
                }
            }
        })
    }

    /**
     * Procesa el cobro bancario real:
     * 1. Descuenta del saldo del usuario.
     * 2. Transfiere a la cuenta de referencia.
     */
    fun procesarCobroBancario(monto: Double, onResult: (Boolean, String) -> Unit) {
        val uid = currentUserId
        if (uid == "INVITADO" || uid.isEmpty()) {
            onResult(false, "Inicia sesión para pagar")
            return
        }

        if (userBalance < monto) {
            onResult(false, "Saldo insuficiente en tu cuenta bancaria")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("usuarios").document(uid)
        val targetAccountRef = db.collection("banco_oficial").document("722969013635365979")

        // Usamos una transacción para asegurar que el dinero no se pierda ni se duplique
        db.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            val currentBalance = userDoc.getDouble("saldo") ?: 0.0
            
            if (currentBalance < monto) {
                throw Exception("Saldo insuficiente")
            }

            // 1. Descontar del usuario
            transaction.update(userRef, "saldo", currentBalance - monto)

            // 2. Aumentar a la cuenta destino (Simulado)
            // Si el documento no existe, lo creamos
            transaction.set(targetAccountRef, mapOf(
                "saldo_acumulado" to FieldValue.increment(monto),
                "ultima_transaccion" to System.currentTimeMillis()
            ), SetOptions.merge())

            null
        }.addOnSuccessListener {
            userBalance -= monto
            onResult(true, "Cobro realizado con éxito")
        }.addOnFailureListener { e ->
            onResult(false, e.message ?: "Error en la transacción bancaria")
        }
    }

    fun updateProfileImage(imageUrl: String) {
        val uid = currentUserId
        if (uid == "INVITADO") return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).update("imageUrl", imageUrl)
            .addOnSuccessListener {
                userImageUrl = imageUrl
            }
    }

    fun addToCart(product: HomeScreenKtProduct) {
        val existing = cartItems.find { it.id == product.id }
        if (existing != null) {
            cartItems = cartItems.map { if (it.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            cartItems = cartItems + CartItem(product.id, product.nombre, product.categoria, product.precio, product.imagenUrl, 1)
        }
    }

    fun increaseQuantity(itemId: String) {
        cartItems = cartItems.map { if (it.id == itemId) it.copy(quantity = it.quantity + 1) else it }
    }

    fun decreaseQuantity(itemId: String) {
        val item = cartItems.find { it.id == itemId } ?: return
        if (item.quantity > 1) {
            cartItems = cartItems.map { if (it.id == itemId) it.copy(quantity = it.quantity - 1) else it }
        } else {
            cartItems = cartItems.filter { it.id != itemId }
        }
    }

    fun updateQuantity(itemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            cartItems = cartItems.filter { it.id != itemId }
        } else {
            cartItems = cartItems.map { if (it.id == itemId) it.copy(quantity = newQuantity) else it }
        }
    }

    fun clearCart() { cartItems = emptyList() }

    fun toggleDarkMode(enabled: Boolean) { isDarkMode = enabled }
    fun toggleNotifications(enabled: Boolean) { notificationsEnabled = enabled }

    fun registrarTokenFCM() {
        if (currentUserId == "INVITADO" || currentUserId.isEmpty()) return

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("AppViewModel", "Error al obtener token FCM de Firebase", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (token != null) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("usuarios").document(currentUserId)
                        .set(mapOf("fcmToken" to token), SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("AppViewModel", "Token FCM guardado exitosamente en Firestore para $currentUserId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AppViewModel", "Error al guardar token FCM en Firestore", e)
                        }
                }
            }
    }

    /**
     * Cierre de sesión seguro:
     * 1. Elimina el fcmToken de Firebase
     * 2. Destruye Listeners (Fuga de estado)
     * 3. Limpia RAM (Variables de sesión)
     */
    fun limpiarDatosDeSesion(onComplete: () -> Unit = {}) {
        val uid = currentUserId
        
        val performCleanupAndNavigate = {
            // 1. Destrucción de Listeners
            ordersListenerRegistration?.remove()
            ordersListenerRegistration = null

            // 2. Limpieza de Variables en Memoria
            currentUserId = "INVITADO"
            userName = ""
            userEmail = ""
            userPhone = ""
            userImageUrl = ""
            userAddress = ""
            userAddressesList = emptyList()
            cartItems = emptyList()
            ordersList = emptyList()
            resetTempPaymentData()
            userBalance = 0.0

            // 3. Ejecutar navegación al Login
            onComplete()
        }

        if (uid != "INVITADO" && uid.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid)
                .update("fcmToken", FieldValue.delete())
                .addOnSuccessListener {
                    Log.d("AppViewModel", "fcmToken eliminado de Firestore")
                    
                    // 3. Revocación de Token FCM de la instancia local
                    FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                        performCleanupAndNavigate()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AppViewModel", "Error al eliminar fcmToken", e)
                    performCleanupAndNavigate()
                }
        } else {
            performCleanupAndNavigate()
        }
    }

    @Composable
    fun getString(key: String): String {
        val context = LocalContext.current
        val resourceKey = "client_$key"
        val resId = context.resources.getIdentifier(resourceKey, "string", context.packageName)
        if (resId != 0) {
            return context.getString(resId)
        }
        return key
    }
}
typealias HomeScreenKtProduct = com.developers.client.Product