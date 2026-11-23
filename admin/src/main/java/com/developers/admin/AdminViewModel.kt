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
import com.google.android.gms.tasks.Task
import java.math.BigDecimal

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

    // Listeners para prevenir State Leaks
    private var stockListener: ListenerRegistration? = null
    internal var warehouse by mutableStateOf<WarehouseLoad>(WarehouseLoad.Loading)
        private set
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

    internal fun retryWarehouse() = iniciarEscuchaGlobal(FirebaseFirestore.getInstance())

    private fun iniciarEscuchaGlobal(db: FirebaseFirestore) {
        stockListener?.remove()
        warehouse = WarehouseLoad.Loading
        stockListener = db.collection("materia_prima").addSnapshotListener { snap, error ->
            if (error != null || snap == null) {
                warehouse = WarehouseLoad.Error
                return@addSnapshotListener
            }
            warehouse = runCatching {
                WarehouseLoad.Ready(snap.documents.mapNotNull { doc ->
                    doc.toObject(MateriaPrima::class.java)?.let { material ->
                        WarehouseItem(material.copy(id = doc.id), (doc.get("nivelCritico") as? Number)?.toDouble())
                    }
                })
            }.getOrElse { WarehouseLoad.Error }
            if (warehouse is WarehouseLoad.Ready) {
                val criticos = snap.documents.filter { 
                    (it.getDouble("cantidadActual") ?: 0.0) <= (it.getDouble("nivelCritico") ?: 0.0) 
                }.map { it.getString("nombre") ?: localizedText(TextR.string.pan_unknown) }
                
                insumosCriticosNombres = criticos
                globalCriticosCount = criticos.size
            }
        }
    }

    internal var isSavingWarehouseStock by mutableStateOf(false)
        private set

    internal fun saveWarehouseMaterial(
        id: String?,
        name: String,
        unit: String,
        minimum: BigDecimal,
        initialQuantity: BigDecimal?,
        onResult: (Int?) -> Unit
    ) {
        if (isSavingWarehouseStock) return
        val materials = FirebaseFirestore.getInstance().collection("materia_prima")
        val fields = mutableMapOf<String, Any>("nombre" to name, "nivelCritico" to minimum.toDouble())
        val task = if (id == null) {
            fields["unidadMedida"] = unit
            fields["cantidadActual"] = requireNotNull(initialQuantity).toDouble()
            fields["codigoBarras"] = ""
            fields["codigosBarras"] = emptyList<String>()
            fields["cantidadesPorCodigo"] = emptyMap<String, Double>()
            fields["colorHex"] = "#4CAF50"
            materials.document().set(fields)
        } else {
            materials.document(id).update(fields)
        }
        observeWarehouseWrite(task, onResult)
    }

    internal fun adjustWarehouseStock(material: MateriaPrima, delta: BigDecimal, onResult: (Int?) -> Unit) {
        if (isSavingWarehouseStock) return
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("materia_prima").document(material.id)
        val task = db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            check(snapshot.exists() && (snapshot.getString("unidadMedida") ?: "g") == material.unidadMedida)
            val current = snapshot.getDouble("cantidadActual") ?: 0.0
            val updated = current.toBigDecimal().add(delta)
            if (updated.signum() < 0) {
                false
            } else {
                val quantity = updated.toDouble()
                check(quantity.isFinite() && quantity != current)
                transaction.update(ref, "cantidadActual", quantity)
                true
            }
        }
        observeWarehouseWrite(task, onResult)
    }

    private fun observeWarehouseWrite(task: Task<*>, onResult: (Int?) -> Unit) {
        isSavingWarehouseStock = true
        task.addOnCompleteListener { result ->
            isSavingWarehouseStock = false
            val error = when {
                !result.isSuccessful -> R.string.warehouse_stock_save_error
                result.result == false -> R.string.warehouse_adjust_insufficient
                else -> null
            }
            onResult(error)
        }
    }

    fun limpiarDatosDeSesion(onComplete: () -> Unit = {}) {
        // 1. Destrucción de Listeners
        stockListener?.remove()
        stockListener = null
        warehouse = WarehouseLoad.Loading
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
