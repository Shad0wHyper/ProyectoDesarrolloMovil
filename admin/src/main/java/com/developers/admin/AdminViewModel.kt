package com.developers.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminViewModel : ViewModel() {
    var currentUserId by mutableStateOf("INVITADO")
    var userEmail by mutableStateOf("")
    var userName by mutableStateOf("")
    var userImageUrl by mutableStateOf("")
    var isDarkMode by mutableStateOf(false)

    // Listeners para prevenir State Leaks
    private var stockListener: ListenerRegistration? = null
    private var pedidosListener: ListenerRegistration? = null
    private var ventasListener: ListenerRegistration? = null
    
    // Variables compartidas pesadas
    var globalCriticosCount by mutableStateOf(0)
    var insumosCriticosNombres by mutableStateOf<List<String>>(emptyList())

    fun setSessionUser(uid: String, email: String) {
        if (uid != "INVITADO" && uid.isNotEmpty()) {
            currentUserId = uid
            userEmail = email

            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userName = doc.getString("nombre") ?: "Administrador"
                        userImageUrl = doc.getString("imageUrl") ?: ""
                    }
                }
            
            iniciarEscuchaGlobal(db)
        } else {
            userName = "Admin Invitado"
        }
    }

    private fun iniciarEscuchaGlobal(db: FirebaseFirestore) {
        stockListener?.remove()
        stockListener = db.collection("materia_prima").addSnapshotListener { snap, _ ->
            if (snap != null) {
                val criticos = snap.documents.filter { 
                    (it.getDouble("cantidadActual") ?: 0.0) <= (it.getDouble("nivelCritico") ?: 0.0) 
                }.map { it.getString("nombre") ?: "Desconocido" }
                
                insumosCriticosNombres = criticos
                globalCriticosCount = criticos.size
            }
        }
    }

    fun limpiarDatosDeSesion(onComplete: () -> Unit = {}) {
        // 1. Destrucción de Listeners
        stockListener?.remove()
        stockListener = null
        pedidosListener?.remove()
        pedidosListener = null
        ventasListener?.remove()
        ventasListener = null

        // 2. Ejecutar navegación al Login primero (evita parpadeos en UI)
        onComplete()

        // 3. Limpiar variables en RAM
        currentUserId = "INVITADO"
        userEmail = ""
        userName = "Cargando..."
        userImageUrl = ""
        globalCriticosCount = 0
        insumosCriticosNombres = emptyList()

        // 4. Desconectar Auth
        FirebaseAuth.getInstance().signOut()
    }
}
