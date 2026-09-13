package com.developers.client

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import org.chromium.IsReadyToPayService
import org.chromium.IsReadyToPayServiceCallback

class SampleIsReadyToPayService : Service() {
    private val binder = object : IsReadyToPayService.Stub() {
        override fun isReadyToPay(callback: IsReadyToPayServiceCallback?, parameters: Bundle?) {
            // Se le informa al solicitante que el servicio de pago está listo
            callback?.handleIsReadyToPay(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
}
