package com.developers.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        PanAppScreenTitle(title)
    }
}

@Composable
fun PanAppScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign? = null,
    maxLines: Int = 1
) {
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(text, locale) {
        val lowercase = text.lowercase(locale)
        val firstLetter = lowercase.indexOfFirst { it.isLetter() }
        if (firstLetter < 0) lowercase else {
            lowercase.replaceRange(
                firstLetter,
                firstLetter + 1,
                lowercase[firstLetter].titlecase(locale)
            )
        }
    }
    Text(
        text = title,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleLarge,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
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
        val interactions = remember { MutableInteractionSource() }
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.panAppPressMotion(interactions),
            interactionSource = interactions
        ) {
            if (avatar != null) {
                avatar(Modifier.size(36.dp).clip(CircleShape))
            } else {
                Icon(Icons.Default.AccountCircle, stringResource(R.string.pan_menu_profile), Modifier.size(32.dp))
            }
        }
        PanAppGlassDropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
fun PanAppAccountSettings(role: PanAppRole, onBack: () -> Unit, onProfile: () -> Unit, onLogout: () -> Unit) {
    Scaffold(
        modifier = Modifier.background(panAppRoleBackdrop(role)),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { PanAppHeaderTitle(stringResource(R.string.pan_menu_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pan_back))
                    }
                }
            )
        }
    ) { padding ->
        PanAppGlassCard(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
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
}
