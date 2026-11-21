package com.developers.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.developers.core.R

@Composable
fun PanAppLogo(modifier: Modifier = Modifier.size(45.dp)) {
    Image(
        painter = painterResource(R.drawable.newp),
        contentDescription = stringResource(R.string.pan_official_logo),
        contentScale = ContentScale.Fit,
        modifier = modifier.clip(MaterialTheme.shapes.small)
    )
}

@Composable
fun PanAppHeaderTitle(title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PanAppLogo(Modifier.size(36.dp))
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PanAppProfileMenu(
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: (@Composable (Modifier) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            if (avatar != null) {
                avatar(Modifier.size(36.dp).clip(CircleShape))
            } else {
                Icon(Icons.Default.AccountCircle, stringResource(R.string.pan_menu_profile), Modifier.size(32.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pan_menu_profile)) },
                onClick = { expanded = false; onProfile() },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pan_menu_settings)) },
                onClick = { expanded = false; onSettings() },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
            )
            HorizontalDivider(color = colors.outlineVariant)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pan_menu_logout), color = colors.error) },
                onClick = { expanded = false; onLogout() },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = colors.error) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanAppAccountSettings(onBack: () -> Unit, onProfile: () -> Unit, onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { PanAppHeaderTitle(stringResource(R.string.pan_menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pan_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            PanAppLanguageSelector()
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pan_menu_profile)) },
                onClick = onProfile,
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pan_menu_logout), color = MaterialTheme.colorScheme.error) },
                onClick = onLogout,
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}
