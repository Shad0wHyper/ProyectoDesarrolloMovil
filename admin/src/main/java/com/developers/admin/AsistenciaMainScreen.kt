package com.developers.admin

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

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA)
    val textColor = if (isDarkMode) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding() // ✨ Evita que el título se empalme con la barra de estado
    ) {
        // ✨ Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.admin_nav_attendance),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
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
            // Pill 1: Historial
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (tabIndex == 0) (if (isDarkMode) AdminPrimary.copy(alpha = 0.2f) else Color(0xFFF3E5F5)) else Color.Transparent,
                border = if (tabIndex == 0) null else BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.clickable { tabIndex = 0 }
            ) {
                Text(
                    text = stringResource(R.string.admin_attendance_history),
                    color = if (tabIndex == 0) (if (isDarkMode) AdminPrimary else Color(0xFF6200EE)) else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Pill 2: Generador QR
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (tabIndex == 1) (if (isDarkMode) AdminPrimary.copy(alpha = 0.2f) else Color(0xFFF3E5F5)) else Color.Transparent,
                border = if (tabIndex == 1) null else BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.clickable { tabIndex = 1 }
            ) {
                Text(
                    text = stringResource(R.string.admin_qr_attendance),
                    color = if (tabIndex == 1) (if (isDarkMode) AdminPrimary else Color(0xFF6200EE)) else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✨ Contenido
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (tabIndex == 0) {
                AttendanceHistoryScreen(onBack = {}, isInsideTab = true) 
            } else {
                QrGeneratorScreen(onBack = {}, isInsideTab = true)
            }
        }
    }
}
