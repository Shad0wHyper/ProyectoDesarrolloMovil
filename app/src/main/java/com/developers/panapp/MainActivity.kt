package com.developers.panapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.developers.panapp.ui.theme.PanAppTheme
import com.developers.client.PaymentConfig
import com.stripe.android.PaymentConfiguration

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Transparent system bars let the Compose surface supply the theme background.
        // Activity updates status/navigation bar icon contrast for the system theme.
        enableEdgeToEdge()
        
        // Inicializar Stripe globalmente (heredado del módulo cliente)
        PaymentConfiguration.init(
            applicationContext,
            PaymentConfig.PUBLISHABLE_KEY
        )

        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect {
                // Apply after AppCompat has installed its theme, using the same mode as Compose.
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            PanAppTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().statusBarsPadding()) {
                        Box(Modifier.weight(1f)) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}

