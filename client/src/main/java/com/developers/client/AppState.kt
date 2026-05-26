package com.developers.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf

class AppViewModel : ViewModel() {
    var isDarkMode by mutableStateOf(false)
    var currentLanguage by mutableStateOf("Español")
    var notificationsEnabled by mutableStateOf(true)
    
    var userName by mutableStateOf("Marco Antonio")
    var userEmail by mutableStateOf("marco.antonio@email.com")
    var userPhone by mutableStateOf("+52 555 123 4567")

    val cartItems = mutableStateListOf<Product>()

    fun addToCart(product: Product) {
        cartItems.add(product)
    }

    fun removeFromCart(product: Product) {
        cartItems.remove(product)
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
    }
    
    fun changeLanguage(language: String) {
        currentLanguage = language
    }

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
    }
    
    fun updateProfile(name: String, email: String, phone: String) {
        userName = name
        userEmail = email
        userPhone = phone
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
                "payment_form" to "Formulario de pago para"
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
                "payment_form" to "Payment form for"
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
                "payment_form" to "Formulário de pagamento para"
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
                "payment_form" to "Modulo di pagamento per"
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
                "payment_form" to "Formulaire de paiement pour"
            )
        )
        return translations[currentLanguage]?.get(key) ?: key
    }
}
