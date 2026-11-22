package com.developers.admin

import com.developers.core.ui.PanAppScreenTitle

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.developers.core.ui.PanAppMotion
import com.developers.core.ui.PanAppGlassFilterChip

import com.developers.core.ui.contrastTextColor

import androidx.compose.ui.res.stringResource
import com.developers.admin.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AsistenciaMainScreen() {
    val isDarkMode = isSystemInDarkTheme()
    var tabIndex by remember { mutableIntStateOf(0) } // 0: Historial, 1: Generar QR

    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // ✨ Evita que el título se empalme con la barra de estado
    ) {
        // ✨ Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanAppScreenTitle(
                text = stringResource(R.string.admin_nav_attendance),
                color = textColor
            )
        }

        // ✨ Pestañas Estilo Pill (Copiado de Pedidos para consistencia)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanAppGlassFilterChip(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.admin_attendance_history)) }
            )
            PanAppGlassFilterChip(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.admin_qr_attendance)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✨ Contenido
        Crossfade(
            targetState = tabIndex,
            modifier = Modifier.fillMaxSize().weight(1f),
            animationSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
            label = "attendanceTab"
        ) { tab ->
            if (tab == 0) {
                AttendanceHistoryScreen(onBack = {}, isInsideTab = true) 
            } else {
                QrGeneratorScreen(onBack = {}, isInsideTab = true)
            }
        }
    }
}
