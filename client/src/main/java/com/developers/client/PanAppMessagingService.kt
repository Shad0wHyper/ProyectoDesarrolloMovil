package com.developers.client

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PanAppMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("PanAppMessaging", "Nuevo token FCM generado: $token")
        
        // Si hay un usuario autenticado en Firebase Auth, asociamos el token
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            guardarTokenEnFirestore(currentUser.uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("PanAppMessaging", "Mensaje recibido de: ${remoteMessage.from}")

        // Extraer título y cuerpo de la notificación
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "PanApp Client"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Tienes una nueva notificación"

        mostrarNotificacion(title, body)
    }

    private fun mostrarNotificacion(title: String, body: String) {
        val channelId = "panapp_client_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Crear Canal de Notificación
        val channel = NotificationChannel(
            channelId,
            "Notificaciones de PanApp Cliente",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal para avisos sobre pedidos, ofertas y actualizaciones de cuenta."
        }
        notificationManager.createNotificationChannel(channel)

        // Intent para abrir la MainActivity al presionar la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun guardarTokenEnFirestore(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val data = mapOf("fcmToken" to token)

        db.collection("usuarios").document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("PanAppMessaging", "Token actualizado exitosamente en Firestore para usuario $userId")
            }
            .addOnFailureListener { e ->
                Log.e("PanAppMessaging", "Error al guardar token FCM en Firestore", e)
            }
    }
}
