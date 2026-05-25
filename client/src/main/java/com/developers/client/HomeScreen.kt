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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developers.client.ui.theme.PanAppPeach
import com.developers.client.ui.theme.PanAppPrimary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PanAppPeach),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BakeryDining, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(
                            badge = { Badge { Text("3") } }
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
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Perfil",
                                modifier = Modifier.size(32.dp),
                                tint = if (isDarkMode) Color.White else Color.Black
                            )
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
                                onClick = { showMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color.Black else Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkMode) Color(0xFF121212) else Color.White)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
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
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredProducts) { product ->
                                ProductCard(
                                    name = product.nombre,
                                    price = String.format("$%.2f", product.precio),
                                    rating = product.calificacion.toString(),
                                    isNew = product.isNuevo,
                                    imageUrl = product.imagenUrl,
                                    isDarkMode = isDarkMode,
                                    appViewModel = appViewModel
                                )
                            }
                        }
                    }
                }
            }
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
fun ProductCard(name: String, price: String, rating: String, isNew: Boolean, imageUrl: String, isDarkMode: Boolean, appViewModel: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5))
                )

                if (isNew) {
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
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = (if (isDarkMode) Color.Black else Color.White).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = price,
                        color = PanAppPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(
                        onClick = { /* TODO: Lógica para agregar al carrito */ },
                        modifier = Modifier.size(32.dp).background(PanAppPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
