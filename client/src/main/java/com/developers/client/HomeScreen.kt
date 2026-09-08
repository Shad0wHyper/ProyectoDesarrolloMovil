package com.developers.client

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.developers.client.ui.theme.PanAppPrimary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale

data class Product(
    val id: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val calificacion: Double = 0.0,
    val isNuevo: Boolean = false,
    val categoria: String = "",
    val imagenUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) } // ✨ ESTADO PARA EL BOTTOM SHEET DE DETALLE DE PRODUCTO

    val categories = listOf("TODO", "PANES", "CAFÉ", "OTROS")
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("productos").get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                allProducts = lista
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val isDarkMode = appViewModel.isDarkMode

    Scaffold(
        containerColor = if (isDarkMode) Color(0xFF121212) else Color.White
    ) { padding ->
        // ✨ 1. CONTENEDOR PRINCIPAL QUE OCUPA TODA LA PANTALLA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) Color(0xFF121212) else Color.White)
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
                // LOGO OFICIAL
                Image(
                    painter = painterResource(id = R.drawable.log),
                    contentDescription = "Logo Oficial",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                // ACCIONES DERECHA: CARRITO Y PERFIL
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(
                            badge = {
                                if (appViewModel.cartUniqueItems > 0) {
                                    Badge { Text(appViewModel.cartUniqueItems.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.ShoppingCart,
                                contentDescription = "Carrito",
                                tint = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            if (appViewModel.userImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = appViewModel.userImageUrl,
                                    contentDescription = "Perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = "Perfil",
                                    modifier = Modifier.size(32.dp),
                                    tint = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(if (isDarkMode) Color(0xFF1E1E1E) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text(appViewModel.getString("profile"), color = if (isDarkMode) Color.White else Color.Black) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToProfile()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black) }
                            )
                            DropdownMenuItem(
                                text = { Text(appViewModel.getString("settings"), color = if (isDarkMode) Color.White else Color.Black) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black) }
                            )
                            HorizontalDivider(color = if (isDarkMode) Color.DarkGray else Color.LightGray)
                            DropdownMenuItem(
                                text = { Text(appViewModel.getString("logout"), color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    appViewModel.cerrarSesion()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }

            // ✨ 3. ESPACIADOR CRUCIAL (24.dp) ENTRE HEADER Y SALUDO
            Spacer(modifier = Modifier.height(24.dp))

            // BLOQUE DE SALUDO Y CATEGORÍAS
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "${appViewModel.getString("hello")}, ${appViewModel.userName.split(" ")[0]}! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = appViewModel.getString("what_fancy"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    categories.forEachIndexed { index, catKey ->
                        val translatedName = when (catKey) {
                            "PANES" -> appViewModel.getString("cat_breads")
                            "CAFÉ" -> appViewModel.getString("cat_coffee")
                            "OTROS" -> appViewModel.getString("cat_others")
                            else -> appViewModel.getString("cat_all")
                        }
                        val icon = when (catKey) {
                            "PANES" -> Icons.Default.BakeryDining
                            "CAFÉ" -> Icons.Default.Coffee
                            "OTROS" -> Icons.Default.StarBorder
                            else -> Icons.Default.RestaurantMenu
                        }
                        CategoryItem(
                            name = translatedName,
                            icon = icon,
                            isSelected = pagerState.currentPage == index,
                            isDarkMode = isDarkMode
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                val currentCat = categories[pageIndex]
                val filteredProducts = if (currentCat == "TODO") {
                    allProducts
                } else {
                    allProducts.filter { it.categoria.equals(currentCat, ignoreCase = true) }
                }

                val currentCategoryDisplayName = when (currentCat) {
                    "PANES" -> appViewModel.getString("cat_breads")
                    "CAFÉ" -> appViewModel.getString("cat_coffee")
                    "OTROS" -> appViewModel.getString("cat_others")
                    else -> appViewModel.getString("cat_all")
                }

                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Text(
                        text = if (currentCat == "TODO") appViewModel.getString("products") else "${appViewModel.getString("categories_label")}: $currentCategoryDisplayName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PanAppPrimary)
                        }
                    } else if (filteredProducts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay productos en esta categoría.", color = Color.Gray)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredProducts,
                                key = { product -> product.id },
                                contentType = { "Producto" }
                            ) { product ->
                                ProductCard(
                                    product = product,
                                    isDarkMode = isDarkMode,
                                    appViewModel = appViewModel,
                                    onClick = { selectedProduct = product } // ✨ ASIGNA EL PRODUCTO PARA ABRIR EL BOTTOM SHEET
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

        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            ProductDetailBottomSheetContent(
                product = product,
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
fun CategoryItem(name: String, icon: ImageVector, isSelected: Boolean, isDarkMode: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp).clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) PanAppPrimary else if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isSelected) Color.White else Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) PanAppPrimary else Color.Gray
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    isDarkMode: Boolean,
    appViewModel: AppViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedPrice = remember(product.precio) {
        String.format(Locale.getDefault(), "$%.2f", product.precio)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                AsyncImage(
                    model = product.imagenUrl,
                    contentDescription = product.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5))
                )

                if (product.isNuevo) {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        color = Color(0xFFFF6B6B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Nuevo",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = (if (isDarkMode) Color.Black else Color.White).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.calificacion.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.nombre,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) Color.White else Color.Black,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedPrice,
                        color = PanAppPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(
                        onClick = { appViewModel.addToCart(product) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(PanAppPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar al carrito",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ✨ COMPONENTE 10/10 PREMIUM PARA EL CONTENIDO DEL BOTTOM SHEET
@Composable
fun ProductDetailBottomSheetContent(
    product: Product,
    isDarkMode: Boolean,
    onAddToCart: () -> Unit
) {
    val formattedPrice = remember(product.precio) {
        String.format(Locale.getDefault(), "$%.2f", product.precio)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                model = product.imagenUrl,
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
                        text = "NUEVO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                color = (if (isDarkMode) Color.Black else Color.White).copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.calificacion.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
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
                    color = PanAppPrimary,
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
            color = if (isDarkMode) Color.White else Color.Black
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
                    text = "Precio",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PanAppPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                text = "Agregar al Carrito",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
