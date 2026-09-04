package com.developers.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val itemsList: List<OrderItemDetail> = emptyList() // ✨ AQUÍ SE GUARDAN TODOS LOS ARTÍCULOS
)

class AppViewModel : ViewModel() {
    var isDarkMode by mutableStateOf(false)
    var currentLanguage by mutableStateOf("Español")
    var notificationsEnabled by mutableStateOf(true)

    // Datos del Usuario
    var currentUserId by mutableStateOf("INVITADO")
    var userName by mutableStateOf("Cargando...")
    var userEmail by mutableStateOf("Cargando...")
    var userPhone by mutableStateOf("...")
    var userImageUrl by mutableStateOf("")
    var userAddress by mutableStateOf("")

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
                } else {
                    userName = "PanApp User"
                }
            }

        // ESCUCHA EN VIVO DE PEDIDOS
        db.collection("usuarios").document(uid).collection("pedidos")
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
        val updates = mapOf("nombre" to name, "telefono" to phone, "direccion" to address)
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).update(updates)
            .addOnSuccessListener {
                userName = name; userPhone = phone; userAddress = address
                onComplete()
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
    fun changeLanguage(language: String) { currentLanguage = language }
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
     * 1. Elimina el fcmToken de Firestore en usuarios/{currentUserId} para evitar notificaciones fantasma
     * 2. Limpia los datos locales del ViewModel
     * 3. Cierra la sesión en FirebaseAuth
     * 4. Llama al callback onComplete
     */
    fun cerrarSesion(onComplete: () -> Unit = {}) {
        val uid = currentUserId

        val resetLocalState = {
            currentUserId = "INVITADO"
            userName = "Invitado"
            userEmail = "Inicia sesión para ordenar"
            userPhone = "..."
            userImageUrl = ""
            userAddress = ""
            cartItems = emptyList()
            ordersList = emptyList()

            FirebaseAuth.getInstance().signOut()
            onComplete()
        }

        if (uid != "INVITADO" && uid.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid)
                .update("fcmToken", FieldValue.delete())
                .addOnSuccessListener {
                    Log.d("AppViewModel", "fcmToken eliminado de Firestore exitosamente al cerrar sesión")
                    resetLocalState()
                }
                .addOnFailureListener { e ->
                    Log.e("AppViewModel", "Error al eliminar fcmToken de Firestore", e)
                    resetLocalState()
                }
        } else {
            resetLocalState()
        }
    }

    fun getString(key: String): String {

        val translations = mapOf(
            "Español" to mapOf(
                "hello" to "¡Hola",
                "what_fancy" to "¿Qué te apetece hoy?",
                "search_placeholder" to "Busca tu pan favorito...",
                "categories_label" to "CATEGORÍAS",
                "cat_all" to "TODO",
                "cat_breads" to "PANES",
                "cat_coffee" to "CAFÉ",
                "cat_others" to "OTROS",
                "products" to "Nuestros Productos",
                "settings" to "Ajustes",
                "my_orders" to "Mis Pedidos",
                "profile" to "Mi Perfil",
                "logout" to "Cerrar Sesión",
                "dark_mode" to "Modo Nocturno",
                "language" to "Idioma",
                "notifications" to "Notificaciones",
                "save_changes" to "Guardar Cambios",
                "cart" to "Mi Carrito",
                "total" to "Total a pagar",
                "confirm_order" to "Confirmar Pedido",
                "history" to "Historial",
                "your_order" to "Tu Pedido",
                "items" to "artículos",
                "view_details" to "Ver detalles",
                "buy_again" to "Comprar de Nuevo",
                "order_details" to "Detalles del Pedido",
                "date" to "Fecha",
                "close" to "Cerrar",
                "payment_method" to "Método de Pago",
                "pay_now" to "Pagar Ahora",
                "card" to "Tarjeta",
                "transfer" to "Transferencia",
                "cash" to "Efectivo",
                "local_pay" to "Pago Local",
                "preferences" to "Preferencias",
                "account" to "Cuenta",
                "dark_mode_desc" to "Ahorra batería y descansa la vista",
                "profile_desc" to "Edita tu información personal",
                "notif_desc" to "Gestiona tus alertas",
                "full_name" to "Nombre Completo",
                "email_label" to "Correo Electrónico",
                "phone_label" to "Teléfono",
                "pendiente" to "Pendiente",
                "en_camino" to "En Camino",
                "entregado" to "Entregado",
                "payment_form" to "Formulario de pago para",
                "empty_cart" to "Tu carrito está vacío",
                "subtotal" to "Subtotal",
                "delivery" to "Envío",
                "taxes" to "Impuestos"
            ),
            "English" to mapOf(
                "hello" to "Hello",
                "what_fancy" to "What do you fancy today?",
                "search_placeholder" to "Search your favorite bread...",
                "categories_label" to "CATEGORIES",
                "cat_all" to "ALL",
                "cat_breads" to "BREADS",
                "cat_coffee" to "COFFEE",
                "cat_others" to "OTHERS",
                "products" to "Our Products",
                "settings" to "Settings",
                "my_orders" to "My Orders",
                "profile" to "My Profile",
                "logout" to "Logout",
                "dark_mode" to "Dark Mode",
                "language" to "Language",
                "notifications" to "Notifications",
                "save_changes" to "Save Changes",
                "cart" to "My Cart",
                "total" to "Total to pay",
                "confirm_order" to "Confirm Order",
                "history" to "History",
                "your_order" to "Your Order",
                "items" to "items",
                "view_details" to "View details",
                "buy_again" to "Buy Again",
                "order_details" to "Order Details",
                "date" to "Date",
                "close" to "Close",
                "payment_method" to "Payment Method",
                "pay_now" to "Pay Now",
                "card" to "Card",
                "transfer" to "Transfer",
                "cash" to "Cash",
                "local_pay" to "Local Pay",
                "preferences" to "Preferences",
                "account" to "Account",
                "dark_mode_desc" to "Save battery and rest your eyes",
                "profile_desc" to "Edit your personal info",
                "notif_desc" to "Manage your alerts",
                "full_name" to "Full Name",
                "email_label" to "Email Address",
                "phone_label" to "Phone Number",
                "pendiente" to "Pending",
                "en_camino" to "On the way",
                "entregado" to "Delivered",
                "payment_form" to "Payment form for",
                "empty_cart" to "Your cart is empty",
                "subtotal" to "Subtotal",
                "delivery" to "Delivery",
                "taxes" to "Taxes"
            ),
            "Português" to mapOf(
                "hello" to "Olá",
                "what_fancy" to "O que você deseja hoje?",
                "search_placeholder" to "Procure seu pão favorito...",
                "categories_label" to "CATEGORIAS",
                "cat_all" to "TUDO",
                "cat_breads" to "PÃES",
                "cat_coffee" to "CAFÉ",
                "cat_others" to "OUTROS",
                "products" to "Nossos Produtos",
                "settings" to "Ajustes",
                "my_orders" to "Meus Pedidos",
                "profile" to "Meu Perfil",
                "logout" to "Sair",
                "dark_mode" to "Modo Noturno",
                "language" to "Idioma",
                "notifications" to "Notificações",
                "save_changes" to "Salvar Alterações",
                "cart" to "Meu Carrinho",
                "total" to "Total a pagar",
                "confirm_order" to "Confirmar Pedido",
                "history" to "Histórico",
                "your_order" to "Seu Pedido",
                "items" to "itens",
                "view_details" to "Ver detalhes",
                "buy_again" to "Comprar de Novo",
                "order_details" to "Detalhes do Pedido",
                "date" to "Data",
                "close" to "Fechar",
                "payment_method" to "Método de Pagamento",
                "pay_now" to "Pagar Agora",
                "card" to "Cartão",
                "transfer" to "Transferência",
                "cash" to "Dinheiro",
                "local_pay" to "Pagamento Local",
                "preferences" to "Preferências",
                "account" to "Conta",
                "dark_mode_desc" to "Economize bateria e descanse os olhos",
                "profile_desc" to "Edite suas informações pessoais",
                "notif_desc" to "Gerencie seus alertas",
                "full_name" to "Nome Completo",
                "email_label" to "E-mail",
                "phone_label" to "Telefone",
                "pendiente" to "Pendente",
                "en_camino" to "A caminho",
                "entregado" to "Entregue",
                "payment_form" to "Formulário de pagamento para",
                "empty_cart" to "Seu carrinho está vazio",
                "subtotal" to "Subtotal",
                "delivery" to "Entrega",
                "taxes" to "Impostos"
            ),
            "Italiano" to mapOf(
                "hello" to "Ciao",
                "what_fancy" to "Cosa desideri oggi?",
                "search_placeholder" to "Cerca il tuo pane preferito...",
                "categories_label" to "CATEGORIE",
                "cat_all" to "TUTTO",
                "cat_breads" to "PANE",
                "cat_coffee" to "CAFFÈ",
                "cat_others" to "ALTRO",
                "products" to "I Nostri Prodotti",
                "settings" to "Impostazioni",
                "my_orders" to "I Miei Ordini",
                "profile" to "Il Mio Profilo",
                "logout" to "Disconnetti",
                "dark_mode" to "Modalità Scura",
                "language" to "Lingua",
                "notifications" to "Notifiche",
                "save_changes" to "Salva Modifiche",
                "cart" to "Il Mio Carrello",
                "total" to "Totale da pagare",
                "confirm_order" to "Conferma Ordine",
                "history" to "Cronologia",
                "your_order" to "Il Tuo Ordine",
                "items" to "articoli",
                "view_details" to "Vedi dettagli",
                "buy_again" to "Compra di Nuovo",
                "order_details" to "Dettagli Ordine",
                "date" to "Data",
                "close" to "Chiudi",
                "payment_method" to "Metodo di Pagamento",
                "pay_now" to "Paga Ora",
                "card" to "Carta",
                "transfer" to "Bonifico",
                "cash" to "Contanti",
                "local_pay" to "Pagamento Locale",
                "preferences" to "Preferenze",
                "account" to "Account",
                "dark_mode_desc" to "Risparmia batteria e riposa gli occhi",
                "profile_desc" to "Modifica le tue info personali",
                "notif_desc" to "Gestisci i tuoi avvisi",
                "full_name" to "Nome Completo",
                "email_label" to "Indirizzo Email",
                "phone_label" to "Numero di Telefono",
                "pendiente" to "In attesa",
                "en_camino" to "In viaggio",
                "entregado" to "Consegnato",
                "payment_form" to "Modulo di pagamento per",
                "empty_cart" to "Il tuo carrello è vuoto",
                "subtotal" to "Subtotale",
                "delivery" to "Consegna",
                "taxes" to "Tasse"
            ),
            "Français" to mapOf(
                "hello" to "Bonjour",
                "what_fancy" to "Qu'est-ce qui vous ferait plaisir aujourd'hui ?",
                "search_placeholder" to "Cherchez votre pain préféré...",
                "categories_label" to "CATÉGORIES",
                "cat_all" to "TOUT",
                "cat_breads" to "PAINS",
                "cat_coffee" to "CAFÉ",
                "cat_others" to "AUTRES",
                "products" to "Nos Produits",
                "settings" to "Paramètres",
                "my_orders" to "Mes Commandes",
                "profile" to "Mon Profil",
                "logout" to "Déconnexion",
                "dark_mode" to "Mode Sombre",
                "language" to "Langue",
                "notifications" to "Notifications",
                "save_changes" to "Enregistrer les modifications",
                "cart" to "Mon Panier",
                "total" to "Total à payer",
                "confirm_order" to "Confirmer la commande",
                "history" to "Historique",
                "your_order" to "Votre Commande",
                "items" to "articles",
                "view_details" to "Voir les detalles",
                "buy_again" to "Commander à nouveau",
                "order_details" to "Détails de la commande",
                "date" to "Date",
                "close" to "Fermer",
                "payment_method" to "Mode de Paiement",
                "pay_now" to "Payer Maintenant",
                "card" to "Carte",
                "transfer" to "Virement",
                "cash" to "Espèces",
                "local_pay" to "Paiement Local",
                "preferences" to "Préférences",
                "account" to "Compte",
                "dark_mode_desc" to "Économisez la batterie et reposez vos yeux",
                "profile_desc" to "Modifiez vos infos personnelles",
                "notif_desc" to "Gérez vos alertes",
                "full_name" to "Nom Complet",
                "email_label" to "Adresse Email",
                "phone_label" to "Numéro de Téléphone",
                "pendiente" to "En attente",
                "en_camino" to "En chemin",
                "entregado" to "Livré",
                "payment_form" to "Formulaire de paiement pour",
                "empty_cart" to "Votre panier est vide",
                "subtotal" to "Sous-total",
                "delivery" to "Livraison",
                "taxes" to "Taxes"
            )
        )
        return translations[currentLanguage]?.get(key) ?: key
    }
}
typealias HomeScreenKtProduct = com.developers.client.Product