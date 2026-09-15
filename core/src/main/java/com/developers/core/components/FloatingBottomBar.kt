package com.developers.core.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developers.core.theme.PanAppPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FloatingBottomBar(
    selectedItem: T,
    items: List<Triple<T, String, ImageVector>>,
    onItemClick: (T) -> Unit,
    onSearchClick: (() -> Unit)? = null,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val containerBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    val searchIconColor = if (isDarkMode) Color.White else Color(0xFF2C2C2C)

    Box(
        modifier = modifier
            .wrapContentWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 8.dp, end = 8.dp) // Reducimos padding lateral del contenedor
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Reducimos espacio entre items
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Píldora Principal de Navegación
            Surface(
                shape = CircleShape,
                color = containerBackgroundColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp), // Reducimos padding interno de la píldora
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items.forEach { (item, title, icon) ->
                        val isSelected = selectedItem == item

                        FloatingNavItem(
                            title = title,
                            icon = icon,
                            isSelected = isSelected,
                            isDarkMode = isDarkMode,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }

            // Botón Circular Independiente de Búsqueda (Opcional)
            if (onSearchClick != null) {
                Surface(
                    onClick = onSearchClick,
                    shape = CircleShape,
                    color = containerBackgroundColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(48.dp) // Reducimos de 52dp a 48dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = searchIconColor,
                            modifier = Modifier.size(20.dp) // Reducimos de 22dp a 20dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val activeBackgroundColor = PanAppPrimary.copy(alpha = 0.15f)
    val contentColor = if (isSelected) PanAppPrimary else Color.Gray

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) activeBackgroundColor else Color.Transparent,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = if (isSelected) 12.dp else 10.dp, vertical = 8.dp), // Padding más compacto
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // El icono ahora es la parte central para ahorrar espacio
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium, // Reducimos de labelLarge a labelMedium
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}
