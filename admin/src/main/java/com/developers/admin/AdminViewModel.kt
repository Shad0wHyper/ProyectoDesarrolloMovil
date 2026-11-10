package com.developers.admin

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

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
    // Resolve local fallback text when read, so retained ViewModels follow locale changes.
    private var userNameResource by mutableStateOf<Int?>(null)
    private var userNameValue by mutableStateOf("")
    var userName: String
        get() = userNameResource?.let { localizedText(it) } ?: userNameValue
        set(value) {
            userNameResource = null
            userNameValue = value
        }
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
                    val storedName = doc.getString("nombre")
                    if (storedName == null) userNameResource = TextR.string.pan_admin
                    else userName = storedName
                        userImageUrl = doc.getString("imageUrl") ?: ""
                    }
                }
            
            iniciarEscuchaGlobal(db)
        } else {
            userNameResource = TextR.string.pan_guest_admin
        }
    }

    private fun iniciarEscuchaGlobal(db: FirebaseFirestore) {
        stockListener?.remove()
        stockListener = db.collection("materia_prima").addSnapshotListener { snap, _ ->
            if (snap != null) {
                val criticos = snap.documents.filter { 
                    (it.getDouble("cantidadActual") ?: 0.0) <= (it.getDouble("nivelCritico") ?: 0.0) 
                }.map { it.getString("nombre") ?: localizedText(TextR.string.pan_unknown) }
                
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
        userNameResource = TextR.string.pan_loading
        userImageUrl = ""
        globalCriticosCount = 0
        insumosCriticosNombres = emptyList()

        // 4. Desconectar Auth
        FirebaseAuth.getInstance().signOut()
    }
}
