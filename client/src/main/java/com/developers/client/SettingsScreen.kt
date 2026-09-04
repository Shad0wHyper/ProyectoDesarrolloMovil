package com.developers.client

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
    onNavigateToNotifications: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                    titleContentColor = if (appViewModel.isDarkMode) Color.White else Color.Black,
                    navigationIconContentColor = if (appViewModel.isDarkMode) Color.White else Color.Black
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
                SettingsSectionTitle(appViewModel.getString("preferences"))
                
                SettingsToggleItem(
                    title = appViewModel.getString("dark_mode"),
                    subtitle = appViewModel.getString("dark_mode_desc"),
                    icon = Icons.Default.DarkMode,
                    checked = appViewModel.isDarkMode,
                    onCheckedChange = { appViewModel.toggleDarkMode(it) },
                    isDarkMode = appViewModel.isDarkMode
                )
                
                SettingsClickableItem(
                    title = appViewModel.getString("language"),
                    subtitle = appViewModel.currentLanguage,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true },
                    isDarkMode = appViewModel.isDarkMode
                )

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
                    subtitle = "Cerrar sesión de forma segura",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = {
                        appViewModel.cerrarSesion {
                            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    isDarkMode = appViewModel.isDarkMode
                )
            }
        }
    }


    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(appViewModel.getString("language")) },
            text = {
                Column {
                    listOf("Español", "English", "Português", "Italiano", "Français").forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appViewModel.currentLanguage == language,
                                onClick = { 
                                    appViewModel.changeLanguage(language)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(language)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(appViewModel.getString("close"))
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = PanAppPrimary,
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
            Icon(icon, contentDescription = null, tint = if (isDarkMode) Color.LightGray else Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
            Icon(icon, contentDescription = null, tint = if (isDarkMode) Color.LightGray else Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}
