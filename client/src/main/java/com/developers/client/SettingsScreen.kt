package com.developers.client

import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developers.client.ui.theme.PanAppPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appViewModel.getString("settings"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appViewModel.getString("close"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (appViewModel.isDarkMode) Color.Black else Color.White,
                    titleContentColor = contrastTextColor(Color.Black, darkBackground = appViewModel.isDarkMode),
                    navigationIconContentColor = contrastTextColor(Color.Black, darkBackground = appViewModel.isDarkMode)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (appViewModel.isDarkMode) Color(0xFF121212) else Color(0xFFF8F8F8))
        ) {
            item {
                SettingsSectionTitle(appViewModel.getString("account"))
                
                SettingsClickableItem(
                    title = appViewModel.getString("profile"),
                    subtitle = appViewModel.getString("profile_desc"),
                    icon = Icons.Default.Person,
                    onClick = onNavigateToProfile,
                    isDarkMode = appViewModel.isDarkMode
                )
                
                SettingsClickableItem(
                    title = appViewModel.getString("notifications"),
                    subtitle = appViewModel.getString("notif_desc"),
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotifications,
                    isDarkMode = appViewModel.isDarkMode
                )

                val context = LocalContext.current
                SettingsClickableItem(
                    title = appViewModel.getString("logout"),
                    subtitle = appViewModel.getString("logout_desc"),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = {
                        appViewModel.limpiarDatosDeSesion {
                            Toast.makeText(context, localizedText(TextR.string.pan_logout_success), Toast.LENGTH_SHORT).show()
                            onLogoutClick()
                        }
                    },
                    isDarkMode = appViewModel.isDarkMode
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = contrastAccentColor(PanAppPrimary),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contrastTextColor(Color.Gray, secondary = true, darkBackground = isDarkMode))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = contrastTextColor(Color.Black, darkBackground = isDarkMode))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contrastTextColor(Color.Gray, secondary = true))
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = PanAppPrimary)
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contrastTextColor(Color.Gray, secondary = true, darkBackground = isDarkMode))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = contrastTextColor(Color.Black, darkBackground = isDarkMode))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contrastTextColor(Color.Gray, secondary = true))
            }
            Icon(Icons.Default.Person, contentDescription = null, tint = contrastTextColor(Color.LightGray, secondary = true), modifier = Modifier.size(20.dp))
        }
    }
}
