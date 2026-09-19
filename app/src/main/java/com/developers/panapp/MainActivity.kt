package com.developers.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.developers.panapp.ui.theme.PanAppTheme
import com.developers.client.PaymentConfig
import com.stripe.android.PaymentConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Inicializar Stripe globalmente (heredado del módulo cliente)
        PaymentConfiguration.init(
            applicationContext,
            PaymentConfig.PUBLISHABLE_KEY
        )

        setContent {
            PanAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

