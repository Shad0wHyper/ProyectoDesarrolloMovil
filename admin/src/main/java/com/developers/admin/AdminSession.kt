package com.developers.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.FirebaseFirestore

object AdminSession {
    var userId by mutableStateOf("INVITADO")
    var userEmail by mutableStateOf("")
    var userName by mutableStateOf("")
    var userImageUrl by mutableStateOf("")

    fun initialize(uid: String, email: String) {
        if (uid.isEmpty() || uid == "INVITADO") return
        userId = uid
        userEmail = email

        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userName = doc.getString("nombre") ?: ""
                    userImageUrl = doc.getString("imageUrl") ?: ""
                }
            }
    }

    fun clear() {
        userId = "INVITADO"
        userEmail = ""
        userName = ""
        userImageUrl = ""
    }
}
