package com.developers.client

import androidx.compose.animation.AnimatedVisibility
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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    appViewModel: AppViewModel,
    onNavigateToCart: () -> Unit
) {
    val isDarkMode = appViewModel.isDarkMode
    var query by remember { mutableStateOf("") }
    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Bottom sheet state for product details
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("productos").get()
            .addOnSuccessListener { result ->
                allProducts = result.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                isLoading = false
                // Auto focus search bar
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val searchResults = remember(query, allProducts) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allProducts.filter {
                it.nombre.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) ||
                it.categoria.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkMode) Color(0xFF121212) else Color.White)
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
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF3F4F6),
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Cart Icon Button
                    Box(modifier = Modifier.clickable { onNavigateToCart() }) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = appViewModel.getString("cart"),
                                modifier = Modifier.padding(12.dp),
                                tint = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                        if (appViewModel.cartUniqueItems > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(20.dp),
                                shape = CircleShape,
                                color = Color.Red,
                                border = BorderStroke(2.dp, if (isDarkMode) Color(0xFF121212) else Color.White)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = appViewModel.cartUniqueItems.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (isDarkMode) Color(0xFF121212) else Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PanAppPrimary
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
                        tint = if (isDarkMode) Color.DarkGray else Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        appViewModel.getString("search_hint_empty"),
                        textAlign = TextAlign.Center,
                        color = if (isDarkMode) Color.Gray else Color.DarkGray,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (searchResults.isEmpty()) {
                // Sin resultados
                Column(
                    modifier = Modifier.fillMaxSize(),
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
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Text(
                        appViewModel.getString("search_try_again"),
                        color = Color.Gray
                    )
                }
            } else {
                // Resultados
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResults, key = { it.id }) { product ->
                        SearchProductCard(
                            product = product,
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
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ) {
            ProductDetailBottomSheetContent(
                product = selectedProduct!!,
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
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFF3F4F6))
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
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.getDefault(), "$%.2f", product.precio),
                    color = PanAppPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
