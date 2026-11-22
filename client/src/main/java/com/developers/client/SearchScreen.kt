package com.developers.client

import com.developers.core.ui.panAppFilteredItem

import com.developers.core.ui.PanAppGlassSheet

import com.developers.core.ui.PanAppGlassCard


import com.developers.core.ui.isPanAppInDarkTheme
import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.developers.core.ui.PanAppMotion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developers.client.ui.theme.PanAppPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    appViewModel: AppViewModel,
    onNavigateToCart: () -> Unit
) {
    val isDarkMode = isPanAppInDarkTheme()
    var query by rememberSaveable { mutableStateOf("") }
    val catalog = rememberCatalogProducts()
    val allProducts = catalog.products
    val isLoading = catalog.loading
    var sort by rememberSaveable { mutableStateOf(CatalogSort.Default) }
    val ratings = rememberCatalogRatings(allProducts, appViewModel.currentUserId, appViewModel.ratingRevision)

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Bottom sheet state for product details
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading && catalog.loaded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val searchLocale = Locale.getDefault()
    val searchResults = remember(query, allProducts, searchLocale) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.lowercase(searchLocale)
            allProducts.filter {
                it.nombre.lowercase(searchLocale).contains(normalizedQuery) ||
                    it.categoria.lowercase(searchLocale).contains(normalizedQuery)
            }
        }
    }
    val rankedResults = remember(searchResults, sort, ratings) {
        rankProducts(searchResults, sort, ratings)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text(appViewModel.getString("what_fancy")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = appViewModel.getString("nav_search")) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = appViewModel.getString("clear"))
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = contrastFieldColors(TextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF3F4F6),
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Cart Icon Button
                    Box(modifier = Modifier.clickable { onNavigateToCart() }) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = appViewModel.getString("cart"),
                                modifier = Modifier.padding(12.dp),
                                tint = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                            )
                        }
                        Crossfade(
                            appViewModel.cartTotalQuantity,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp),
                            animationSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing),
                            label = "search cart badge"
                        ) { count ->
                            if (count > 0) Badge { Text(count.toString()) }
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = contrastAccentColor(PanAppPrimary)
                )
            } else if (query.isBlank()) {
                // Sugerencias o estado inicial
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = contrastTextColor(Color.LightGray, secondary = true, darkBackground = isDarkMode)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        appViewModel.getString("search_hint_empty"),
                        textAlign = TextAlign.Center,
                        color = if (isDarkMode) contrastTextColor(Color.Gray, secondary = true) else Color.DarkGray,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                // Resultados
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(
                        key = "searchSort",
                        contentType = "sort",
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                    ) {
                        CatalogSortChips(sort, onSelect = { sort = it })
                    }
                    if (searchResults.isEmpty()) {
                        item(
                            key = "emptySearch",
                            contentType = "empty",
                            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                        ) {
                        Column(
                            modifier = panAppFilteredItem().fillMaxWidth().heightIn(min = 240.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "🍞",
                                fontSize = 60.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "${appViewModel.getString("search_not_found")} '$query'",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                            )
                            Text(
                                appViewModel.getString("search_try_again"),
                                color = contrastTextColor(Color.Gray, secondary = true)
                            )
                        }
                        }
                    }
                    items(
                        items = rankedResults,
                        key = { it.id },
                        contentType = { "product" }
                    ) { product ->
                        SearchProductCard(
                            product = product,
                            modifier = panAppFilteredItem(),
                            rating = ratings[product.id],
                            isDarkMode = isDarkMode,
                            onClick = {
                                keyboardController?.hide()
                                selectedProduct = product
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedProduct != null) {
        PanAppGlassSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            ProductDetailBottomSheetContent(
                product = selectedProduct!!,
                rating = ratings[selectedProduct!!.id],
                isDarkMode = isDarkMode,
                onAddToCart = {
                    appViewModel.addToCart(selectedProduct!!)
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
fun SearchProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    rating: CatalogRating? = null,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    PanAppGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (product.imagenUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imagenUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CatalogPhotoRating(rating, Modifier.align(Alignment.BottomEnd))
            }
            
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = product.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
                CatalogRatingLabel(rating)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.getDefault(), "$%.2f", product.precio),
                    color = contrastAccentColor(PanAppPrimary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
