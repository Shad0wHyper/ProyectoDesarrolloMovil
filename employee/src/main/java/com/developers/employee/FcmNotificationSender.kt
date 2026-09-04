package com.developers.employee

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FcmNotificationSender {

    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Lee el archivo service-account.json desde la carpeta assets y genera un OAuth2 Access Token
     */
    fun obtenerOAuth2Token(context: Context): String? {
        return try {
            val inputStream = context.assets.open("service-account.json")
            val googleCredentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            googleCredentials.refreshIfExpired()
            val tokenValue = googleCredentials.accessToken?.tokenValue
            Log.d("FcmSender", "Token OAuth2 generado exitosamente con service-account.json")
            tokenValue
        } catch (e: Exception) {
            Log.e("FcmSender", "Error al leer service-account.json de assets: ${e.message}")
            null
        }
    }

    /**
     * Consulta el fcmToken del cliente en Firestore en usuarios/{userId}
     */
    fun obtenerFcmTokenCliente(userId: String, onTokenObtenido: (String) -> Unit) {
        if (userId.isEmpty()) {
            Log.w("FcmSender", "El userId está vacío, no se puede obtener el FCM token")
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                if (!token.isNullOrEmpty()) {
                    Log.d("FcmSender", "Token FCM recuperado para cliente $userId: $token")
                    onTokenObtenido(token)
                } else {
                    Log.w("FcmSender", "El usuario $userId no tiene un fcmToken registrado en Firestore")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FcmSender", "Error al leer fcmToken del usuario $userId", e)
            }
    }

    /**
     * Construye el cuerpo de la petición JSON en el formato requerido por FCM HTTP v1 API
     */
    fun construirPayloadJson(fcmToken: String, titulo: String, cuerpo: String, pedidoId: String, nuevoEstado: String): String {
        val messageObject = JSONObject().apply {
            put("token", fcmToken)
            put("notification", JSONObject().apply {
                put("title", titulo)
                put("body", cuerpo)
            })
            put("data", JSONObject().apply {
                put("pedidoId", pedidoId)
                put("nuevoEstado", nuevoEstado)
            })
        }

        return JSONObject().apply {
            put("message", messageObject)
        }.toString()
    }

    /**
     * Envía la notificación Push a la API v1 de FCM usando OkHttp y el OAuth2 Bearer Token
     */
    suspend fun enviarNotificacionPushHttpV1(
        context: Context,
        projectId: String,
        fcmToken: String,
        titulo: String,
        cuerpo: String,
        pedidoId: String,
        nuevoEstado: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Generar dinámicamente el Access Token desde service-account.json en assets
            val accessToken = obtenerOAuth2Token(context)
            if (accessToken.isNullOrEmpty()) {
                Log.e("FcmSender", "No se pudo obtener el OAuth2 Token. Verifica que 'service-account.json' exista en src/main/assets/")
                return@withContext false
            }

            val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
            val jsonPayload = construirPayloadJson(fcmToken, titulo, cuerpo, pedidoId, nuevoEstado)

            val requestBody = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.d("FcmSender", "Notificación FCM enviada con éxito: $responseBody")
                    true
                } else {
                    Log.e("FcmSender", "Error al enviar notificación FCM (${response.code}): $responseBody")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("FcmSender", "Excepción al enviar notificación FCM", e)
            false
        }
    }
}
