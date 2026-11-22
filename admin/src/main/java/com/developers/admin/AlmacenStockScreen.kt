package com.developers.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.developers.core.R as TextR
import com.developers.core.ui.PanAppGlassCard
import com.developers.core.ui.PanAppGlassDropdown
import com.developers.core.ui.PanAppGlassFilterChip
import com.developers.core.ui.PanAppMotion
import com.developers.core.ui.PanAppScreenTitle
import com.developers.core.ui.PanAppRole
import com.developers.core.ui.panAppRoleBackdrop
import com.developers.core.ui.isPanAppInDarkTheme
import com.developers.core.ui.panAppFilteredItem
import com.developers.core.ui.panAppPressMotion
import java.text.NumberFormat

private enum class StockFilter(val label: Int) {
    All(R.string.warehouse_filter_all),
    Alerts(R.string.warehouse_filter_alerts),
    Available(R.string.warehouse_filter_available)
}

private enum class StockEditor { Add, Edit, Adjust }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlmacenStockScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StockFilter.All) }
    var filtersOpen by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<StockEditor?>(null) }
    var selectedMaterial by remember { mutableStateOf<MateriaPrima?>(null) }
    val warehouse = viewModel.warehouse
    val materials = remember(warehouse) {
        (warehouse as? WarehouseLoad.Ready)?.items.orEmpty().map { it.material }
    }
    val alerts = remember(materials) { materials.count { it.hasStockAlert() } }
    val filtered = remember(materials, query, filter) {
        val search = query.trim()
        materials.filter { material ->
            val matchesSearch = material.nombre.contains(search, ignoreCase = true) ||
                material.warehouseBarcodes().any { it.contains(search, ignoreCase = true) }
            matchesSearch && when (filter) {
                StockFilter.All -> true
                StockFilter.Alerts -> material.hasStockAlert()
                StockFilter.Available -> !material.hasStockAlert()
            }
        }
    }

    Scaffold(
        modifier = Modifier.background(warehouseBackdrop()),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { PanAppScreenTitle(stringResource(R.string.warehouse_stock_title), maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.admin_back))
                    }
                },
                actions = {
                    val interaction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { selectedMaterial = null; editor = StockEditor.Add },
                        enabled = warehouse is WarehouseLoad.Ready && !viewModel.isSavingWarehouseStock,
                        modifier = Modifier.padding(end = 12.dp).panAppPressMotion(interaction),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        interactionSource = interaction
                    ) {
                        Icon(Icons.Default.AddCircle, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.admin_new_material),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.widthIn(max = 88.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (warehouse) {
                WarehouseLoad.Loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                WarehouseLoad.Error -> item(key = "error") {
                    PanAppGlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.warehouse_stock_load_error), color = colors.error)
                            TextButton(onClick = viewModel::retryWarehouse) {
                                Text(stringResource(R.string.warehouse_stock_retry))
                            }
                        }
                    }
                }
                is WarehouseLoad.Ready -> {
                    item(key = "summary", contentType = "summary") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.admin_showing_materials, filtered.size),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant
                            )
                            if (alerts > 0) {
                                StockPill(stringResource(R.string.admin_stock_alerts_count, alerts), colors.error)
                            }
                        }
                    }
                    item(key = "search", contentType = "search") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(stringResource(R.string.warehouse_search_material), maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                shape = MaterialTheme.shapes.large,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surface.copy(alpha = 0.65f),
                                    unfocusedContainerColor = colors.surface.copy(alpha = 0.55f),
                                    unfocusedBorderColor = colors.outlineVariant
                                )
                            )
                            Box {
                                PanAppGlassFilterChip(
                                    selected = filter != StockFilter.All,
                                    onClick = { filtersOpen = true },
                                    leadingIcon = { Icon(Icons.Default.Tune, null, Modifier.size(16.dp)) },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(filter.label))
                                            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                        }
                                    }
                                )
                                PanAppGlassDropdown(filtersOpen, { filtersOpen = false }) {
                                    StockFilter.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(option.label)) },
                                            onClick = { filter = option; filtersOpen = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item(key = "empty") {
                            Text(stringResource(R.string.warehouse_stock_empty), color = colors.onSurfaceVariant)
                        }
                    }
                    items(filtered, key = { it.id }, contentType = { "material" }) { material ->
                        MaterialStockCard(
                            material = material,
                            modifier = panAppFilteredItem(),
                            onAdjust = { selectedMaterial = material; editor = StockEditor.Adjust },
                            onEdit = { selectedMaterial = material; editor = StockEditor.Edit }
                        )
                    }
                }
            }
        }
    }

    editor?.let { mode ->
        StockEditorDialog(
            mode = mode,
            material = selectedMaterial,
            viewModel = viewModel,
            onClose = { editor = null }
        )
    }
}

@Composable
private fun MaterialStockCard(
    material: MateriaPrima,
    modifier: Modifier,
    onAdjust: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val color = warehouseStockColor(material)
    val status = when {
        material.cantidadActual <= 0 -> TextR.string.pan_status_out
        material.hasStockAlert() -> TextR.string.pan_status_low
        else -> TextR.string.pan_status_ok
    }
    val codes = remember(material.codigoBarras, material.codigosBarras) { material.warehouseBarcodes() }
    val target = material.nivelCritico.takeIf { it > 0 } ?: 1.0
    val progress = animateFloatAsState(
        targetValue = (material.cantidadActual / target).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
        label = "warehouseStock"
    )
    PanAppGlassCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(material.nombre, style = MaterialTheme.typography.titleMedium)
                    if (codes.isNotEmpty()) {
                        Text(
                            stringResource(R.string.admin_code_short, codes.first()),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                StockPill(stringResource(status), color)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.admin_current_stock), style = MaterialTheme.typography.labelSmall)
                    Text(
                        stringResource(R.string.admin_stock_unit, warehouseQuantity(material.cantidadActual), material.unidadMedida),
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.admin_min_required_caps), style = MaterialTheme.typography.labelSmall)
                    Text(
                        stringResource(R.string.admin_stock_unit, warehouseQuantity(material.nivelCritico), material.unidadMedida),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WarehouseAction(stringResource(R.string.admin_adjust), Icons.Default.Tune, onAdjust, Modifier.weight(1f), highlighted = true)
                WarehouseAction(stringResource(R.string.admin_edit), Icons.Default.Edit, onEdit, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StockEditorDialog(
    mode: StockEditor,
    material: MateriaPrima?,
    viewModel: AdminViewModel,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf(material?.nombre.orEmpty()) }
    var minimum by remember { mutableStateOf(material?.nivelCritico?.toBigDecimal()?.inventoryInput() ?: "0") }
    var unit by remember { mutableStateOf(material?.unidadMedida ?: InventoryUnit.Gram.symbol) }
    var amount by remember { mutableStateOf(if (mode == StockEditor.Add) "0" else "") }
    var remove by remember { mutableStateOf(true) }
    var unitMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val saving = viewModel.isSavingWarehouseStock
    val parsedAmount = inputNumber(amount)
    val parsedMinimum = inputNumber(minimum)
    val canSave = when (mode) {
        StockEditor.Adjust -> parsedAmount?.signum() == 1 && material != null
        StockEditor.Add -> name.isNotBlank() && parsedMinimum != null && parsedAmount != null
        StockEditor.Edit -> name.isNotBlank() && parsedMinimum != null && material != null
    }
    val title = when (mode) {
        StockEditor.Add -> stringResource(R.string.admin_new_material)
        StockEditor.Edit -> stringResource(R.string.warehouse_edit_material)
        StockEditor.Adjust -> stringResource(R.string.admin_adjust_stock_title, material?.nombre.orEmpty())
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onClose() },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mode == StockEditor.Adjust) {
                    material?.let {
                        Text(stringResource(R.string.warehouse_adjust_current, warehouseQuantity(it.cantidadActual), unit))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanAppGlassFilterChip(remove, { remove = true; error = null }, { Text(stringResource(R.string.warehouse_adjust_remove)) })
                        PanAppGlassFilterChip(!remove, { remove = false; error = null }, { Text(stringResource(R.string.warehouse_adjust_add)) })
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.admin_name)) },
                        enabled = !saving,
                        singleLine = true
                    )
                    if (mode == StockEditor.Add) {
                        Box {
                            OutlinedButton(onClick = { unitMenu = true }, enabled = !saving) {
                                Text(stringResource(R.string.warehouse_stock_unit_option, unit))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            PanAppGlassDropdown(unitMenu, { unitMenu = false }) {
                                InventoryUnit.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.symbol) },
                                        onClick = { unit = option.symbol; unitMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    StockNumberField(
                        value = minimum,
                        onValueChange = { minimum = it; error = null },
                        label = stringResource(R.string.admin_min_required),
                        enabled = !saving
                    )
                }
                if (mode != StockEditor.Edit) {
                    StockNumberField(
                        value = amount,
                        onValueChange = { amount = it; error = null },
                        label = stringResource(
                            if (mode == StockEditor.Add) R.string.warehouse_initial_quantity else R.string.warehouse_adjust_quantity,
                            unit
                        ),
                        enabled = !saving
                    )
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave && !saving,
                onClick = {
                    error = null
                    val onResult: (Int?) -> Unit = { failure ->
                        if (failure == null) onClose() else error = failure
                    }
                    when (mode) {
                        StockEditor.Add -> viewModel.saveWarehouseMaterial(null, name.trim(), unit, requireNotNull(parsedMinimum), requireNotNull(parsedAmount), onResult)
                        StockEditor.Edit -> viewModel.saveWarehouseMaterial(requireNotNull(material).id, name.trim(), unit, requireNotNull(parsedMinimum), null, onResult)
                        StockEditor.Adjust -> {
                            val delta = requireNotNull(parsedAmount)
                            viewModel.adjustWarehouseStock(requireNotNull(material), if (remove) delta.negate() else delta, onResult)
                        }
                    }
                }
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.admin_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose, enabled = !saving) { Text(stringResource(R.string.admin_cancel)) }
        }
    )
}

@Composable
private fun StockNumberField(value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        isError = value.isNotBlank() && inputNumber(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun StockPill(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = CircleShape) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
internal fun WarehouseAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.panAppPressMotion(interactions),
        shape = MaterialTheme.shapes.medium,
        interactionSource = interactions,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (highlighted) colors.primaryContainer else colors.surface.copy(alpha = 0.54f),
            contentColor = if (highlighted) colors.onPrimaryContainer else colors.onSurface
        )
    ) {
        Icon(icon, null, Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun warehouseBackdrop(): Brush =
    if (isPanAppInDarkTheme()) {
        panAppRoleBackdrop(PanAppRole.Admin)
    } else {
        Brush.verticalGradient(
            0f to Color(0xFFFFD9C5),
            0.28f to Color(0xFFF3DEE8),
            0.65f to Color(0xFFE1D7F4),
            1f to MaterialTheme.colorScheme.background
        )
    }

internal fun MateriaPrima.warehouseBarcodes(): List<String> =
    (listOf(codigoBarras) + codigosBarras).filter { it.isNotBlank() }.distinct()

internal fun MateriaPrima.hasStockAlert(): Boolean = cantidadActual <= nivelCritico || cantidadActual <= 0

@Composable
internal fun warehouseStockColor(material: MateriaPrima): Color {
    val dark = isPanAppInDarkTheme()
    return when {
        material.cantidadActual <= 0 -> MaterialTheme.colorScheme.error
        material.hasStockAlert() -> if (dark) Color(0xFFFFBC67) else Color(0xFF9A4D00)
        else -> if (dark) Color(0xFF72DB91) else Color(0xFF087632)
    }
}

@Composable
internal fun warehouseQuantity(value: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 6 }
    }
    return formatter.format(value)
}
