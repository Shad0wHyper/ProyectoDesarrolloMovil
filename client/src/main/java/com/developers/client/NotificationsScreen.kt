package com.developers.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val isDarkMode = appViewModel.isDarkMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appViewModel.getString("notifications"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appViewModel.getString("close"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color.Black else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color.Black,
                    navigationIconContentColor = if (isDarkMode) Color.White else Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F8F8))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                imageVector = if (appViewModel.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = if (appViewModel.notificationsEnabled) MaterialTheme.colorScheme.primary else Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = appViewModel.getString("notifications"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            
            Text(
                text = appViewModel.getString("notif_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            SettingsToggleItem(
                title = appViewModel.getString("notifications"),
                subtitle = appViewModel.getString("notif_desc"),
                icon = Icons.Default.Notifications,
                checked = appViewModel.notificationsEnabled,
                onCheckedChange = { appViewModel.toggleNotifications(it) },
                isDarkMode = isDarkMode
            )
        }
    }
}
