package com.developers.client

import com.developers.core.ui.panAppFilteredItem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.developers.core.ui.panAppPanelColor
import com.developers.core.ui.panAppPressMotion
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role

import com.developers.core.ui.PanAppGlassSheet

import com.developers.core.ui.PanAppGlassCard


import com.developers.core.ui.PanAppLogo
import com.developers.core.ui.PanAppProfileMenu

import com.developers.core.ui.isPanAppInDarkTheme
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor
import com.developers.core.ui.contrastErrorColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.developers.core.ui.PanAppMotion
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developers.client.ui.theme.PanAppPrimary
import kotlinx.coroutines.launch
import java.util.Locale

data class Product(
    val id: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val calificacion: Double = 0.0,
    val isNuevo: Boolean = false,
    val categoria: String = "",
    val imagenUrl: String = "",
    val pedidosRegistrados: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) } // ✨ ESTADO PARA EL BOTTOM SHEET DE DETALLE DE PRODUCTO

    val compactHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 480
    val categories = remember { listOf("TODO", "PANES", "CAFÉ", "OTROS") }
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    val catalog = rememberCatalogProducts()
    val allProducts = catalog.products
    val isLoading = catalog.loading
    var sort by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(CatalogSort.Default) }
    val ratings = rememberCatalogRatings(allProducts, appViewModel.currentUserId, appViewModel.ratingRevision)



    val isDarkMode = isPanAppInDarkTheme()

    // ✨ MEMORIZAR NOMBRE DE USUARIO PARA EVITAR .split() EN CADA FRAME DE SCROLL (120Hz)
    val userFirstName = remember(appViewModel.userName) {
        appViewModel.userName.split(" ").firstOrNull() ?: localizedText(TextR.string.pan_customer)
    }

    val catBreads = appViewModel.getString("cat_breads")
    val catCoffee = appViewModel.getString("cat_coffee")
    val catOthers = appViewModel.getString("cat_others")
    val catAll = appViewModel.getString("cat_all")

    // ✨ MEMORIZAR TRADUCCIONES DE CATEGORÍAS
    val categoryItems = remember(catBreads, catCoffee, catOthers, catAll) {
        categories.mapIndexed { index, catKey ->
            val translatedName = when (catKey) {
                "PANES" -> catBreads
                "CAFÉ" -> catCoffee
                "OTROS" -> catOthers
                else -> catAll
            }
            val icon = when (catKey) {
                "PANES" -> Icons.Default.BakeryDining
                "CAFÉ" -> Icons.Default.Coffee
                "OTROS" -> Icons.Default.StarBorder
                else -> Icons.Default.RestaurantMenu
            }
            Triple(index, translatedName, icon)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        // ✨ 1. CONTENEDOR PRINCIPAL QUE OCUPA TODA LA PANTALLA
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ✨ 2. TOP ROW (HEADER) ALINEADO PERFECTAMENTE CON EL STATUS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanAppLogo(Modifier.size(40.dp))

                // ACCIONES DERECHA: CARRITO Y PERFIL
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(
                            badge = {
                                Crossfade(appViewModel.cartTotalQuantity, animationSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing), label = "cart badge") { count ->
                                    if (count > 0) Badge { Text(count.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.ShoppingCart,
                                contentDescription = localizedText(TextR.string.pan_cart),
                                tint = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                            )
                        }
                    }
                    PanAppProfileMenu(
                        onProfile = onNavigateToProfile,
                        onSettings = onNavigateToSettings,
                        onLogout = { appViewModel.limpiarDatosDeSesion { onLogoutClick() } },
                        avatar = if (appViewModel.userImageUrl.isNotEmpty()) {
                            { modifier ->
                                AsyncImage(
                                    model = appViewModel.userImageUrl,
                                    contentDescription = localizedText(TextR.string.pan_profile),
                                    contentScale = ContentScale.Crop,
                                    modifier = modifier
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 4.dp else 24.dp))

            // BLOQUE DE SALUDO Y CATEGORÍAS
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (!compactHeight) {
                    Text(
                        text = "${appViewModel.getString("hello")}, $userFirstName! 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                    )
                    Text(
                        text = appViewModel.getString("what_fancy"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contrastTextColor(Color.Gray, secondary = true)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    categoryItems.forEach { (index, translatedName, icon) ->
                        CategoryItem(
                            name = translatedName,
                            icon = icon,
                            isSelected = pagerState.currentPage == index,
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 24.dp))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                val currentCat = categories[pageIndex]

                val categoryProducts = remember(allProducts, currentCat) {
                    if (currentCat == "TODO") allProducts
                    else allProducts.filter { it.categoria.equals(currentCat, ignoreCase = true) }
                }
                val filteredProducts = remember(categoryProducts, sort, ratings) {
                    rankProducts(categoryProducts, sort, ratings)
                }

                val currentCategoryDisplayName = when (currentCat) {
                    "PANES" -> catBreads
                    "CAFÉ" -> catCoffee
                    "OTROS" -> catOthers
                    else -> catAll
                }

                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Text(
                        text = if (currentCat == "TODO") appViewModel.getString("products") else "${appViewModel.getString("categories_label")}: $currentCategoryDisplayName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                    )

                    CatalogSortChips(sort, onSelect = { sort = it })
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = contrastAccentColor(PanAppPrimary))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(150.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (filteredProducts.isEmpty()) {
                                item(
                                    key = "emptyCategory",
                                    contentType = "empty",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Box(
                                        panAppFilteredItem().fillMaxWidth().heightIn(min = 180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(localizedText(TextR.string.pan_empty_category), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            items(
                                items = filteredProducts,
                                key = { product -> product.id },
                                contentType = { "Producto" }
                            ) { product ->
                                val onCardClick = remember(product) { { selectedProduct = product } }
                                ProductCard(
                                    product = product,
                                    isDarkMode = isDarkMode,
                                    appViewModel = appViewModel,
                                    onClick = onCardClick,
                                    rating = ratings[product.id],
                                    modifier = panAppFilteredItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ✨ MODAL BOTTOM SHEET 10/10 PREMIUM PARA DETALLE DE PRODUCTO
    if (selectedProduct != null) {
        val product = selectedProduct!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        PanAppGlassSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            ProductDetailBottomSheetContent(
                product = product,
                rating = ratings[product.id],
                isDarkMode = isDarkMode,
                onAddToCart = {
                    appViewModel.addToCart(product)
                    selectedProduct = null
                }
            )
        }
    }
}

@Composable
fun CategoryItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    val spec = tween<Color>(PanAppMotion.EnterMillis, easing = PanAppMotion.easing)
    val fill by animateColorAsState(
        if (isSelected) colors.primaryContainer else panAppPanelColor(),
        animationSpec = spec,
        label = "categoryFill"
    )
    val outline by animateColorAsState(
        if (isSelected) colors.primary else colors.outlineVariant,
        animationSpec = spec,
        label = "categoryOutline"
    )
    val shape = MaterialTheme.shapes.medium
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(70.dp)
            .panAppPressMotion(interactions)
            .clip(shape)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                interactionSource = interactions,
                indication = null,
                onClick = onClick
            )
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = shape,
            color = fill,
            border = BorderStroke(1.dp, outline)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.onSurface)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) colors.primary else colors.onSurfaceVariant
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    isDarkMode: Boolean,
    appViewModel: AppViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: CatalogRating? = null
) {
    val context = LocalContext.current

    val formattedPrice = remember(product.precio) {
        String.format(Locale.getDefault(), "$%.2f", product.precio)
    }

    // ✨ CACHÉ OPTIMIZADO DE COIL PARA CADA IMAGEN DE PRODUCTO
    val imageRequest = remember(product.imagenUrl, context) {
        ImageRequest.Builder(context)
            .data(product.imagenUrl)
            .crossfade(true)
            .build()
    }

    val shape = MaterialTheme.shapes.large
    PanAppGlassCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = shape
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = product.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                )

                if (product.isNuevo) {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        color = Color(0xFFFF6B6B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = localizedText(TextR.string.pan_new),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                CatalogPhotoRating(rating, Modifier.align(Alignment.BottomEnd))
            }
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = product.nombre,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode),
                    maxLines = 1
                )
                CatalogRatingLabel(rating)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedPrice,
                        color = contrastAccentColor(PanAppPrimary),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    CartAddIconButton(onClick = { appViewModel.addToCart(product) })
                }
            }
        }
    }
}

// ✨ COMPONENTE 10/10 PREMIUM PARA EL CONTENIDO DEL BOTTOM SHEET
@Composable
fun ProductDetailBottomSheetContent(
    product: Product,
    rating: CatalogRating? = null,
    isDarkMode: Boolean,
    onAddToCart: () -> Unit
) {
    val context = LocalContext.current

    val formattedPrice = remember(product.precio) {
        String.format(Locale.getDefault(), "$%.2f", product.precio)
    }

    val imageRequest = remember(product.imagenUrl, context) {
        ImageRequest.Builder(context)
            .data(product.imagenUrl)
            .crossfade(true)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp, top = 8.dp)
    ) {
        // Imagen Principal Grande
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5))
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = product.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (product.isNuevo) {
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    color = Color(0xFFFF6B6B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = localizedText(TextR.string.pan_new_upper),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


        }

        Spacer(modifier = Modifier.height(20.dp))

        // Categoría y Nombre
        if (product.categoria.isNotEmpty()) {
            Surface(
                color = PanAppPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = product.categoria.uppercase(),
                    color = contrastAccentColor(PanAppPrimary),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = product.nombre,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Precio Destacado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = localizedText(TextR.string.pan_price),
                    style = MaterialTheme.typography.labelMedium,
                    color = contrastTextColor(Color.Gray, secondary = true)
                )
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contrastAccentColor(PanAppPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CatalogRatingLabel(rating)
        Spacer(modifier = Modifier.height(20.dp))

        // Botón Agregar al Carrito Grande
        Button(
            onClick = onAddToCart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = localizedText(TextR.string.pan_add_cart_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
