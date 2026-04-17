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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.client.ui.theme.PanAppPeach
import com.developers.client.ui.theme.PanAppPrimary
import kotlinx.coroutines.launch

data class Product(
    val name: String,
    val price: String,
    val rating: String,
    val isNew: Boolean,
    val category: String
)

val allProducts = listOf(
    Product("Croissant", "$2.50", "4.8", true, "PANES"),
    Product("Baguette", "$1.20", "4.5", false, "PANES"),
    Product("Muffin", "$3.00", "4.7", true, "PANES"),
    Product("Cappuccino", "$3.75", "4.9", false, "CAFÉ"),
    Product("Latte", "$3.50", "4.8", false, "CAFÉ"),
    Product("Espresso", "$2.00", "4.6", false, "CAFÉ"),
    Product("Jugo Naranja", "$2.50", "4.4", false, "OTROS"),
    Product("Té Verde", "$2.20", "4.5", false, "OTROS")
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
                                text = { Text(appViewModel.getString("my_orders"), color = if (isDarkMode) Color.White else Color.Black) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToOrders()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black) }
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

                // Categories Row with Clickable behavior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    categories.forEachIndexed { index, catKey ->
                        val translatedName = when(catKey) {
                            "PANES" -> appViewModel.getString("cat_breads")
                            "CAFÉ" -> appViewModel.getString("cat_coffee")
                            "OTROS" -> appViewModel.getString("cat_others")
                            else -> appViewModel.getString("cat_all")
                        }
                        val icon = when(catKey) {
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

            // Swipeable Pager for Products
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                val currentCat = categories[pageIndex]
                val filteredProducts = if (currentCat == "TODO") {
                    allProducts
                } else {
                    allProducts.filter { it.category == currentCat }
                }

                val currentCategoryDisplayName = when(currentCat) {
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

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProducts) { product ->
                            ProductCard(
                                name = product.name,
                                price = product.price,
                                rating = product.rating,
                                isNew = product.isNew,
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
fun ProductCard(name: String, price: String, rating: String, isNew: Boolean, isDarkMode: Boolean, appViewModel: AppViewModel) {
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
                Box(modifier = Modifier.fillMaxSize().background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)))
                
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
                        onClick = { },
                        modifier = Modifier.size(32.dp).background(PanAppPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
