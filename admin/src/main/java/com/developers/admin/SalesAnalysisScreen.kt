package com.developers.admin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developers.core.sales.readSalesTax
import com.developers.core.sales.salesTaxDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@Composable
internal fun SalesSummaryCard(modifier: Modifier, onClick: () -> Unit) {
    val state = rememberSales(0)
    val month = java.time.YearMonth.now(salesZone)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
            Text(stringResource(R.string.admin_total_sales), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.sales_this_month), style = MaterialTheme.typography.labelMedium)
            when (state) {
                SalesLoad.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                SalesLoad.Error -> Text(stringResource(R.string.sales_error))
                is SalesLoad.Ready -> Text(state.sales.filter { java.time.YearMonth.from(it.date) == month }.sumOf { it.total }.mxn(),
                    style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalesAnalysisScreen(onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    var fromDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var product by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(SalesPeriod.Day) }
    var retry by remember { mutableIntStateOf(0) }
    var selectStart by remember { mutableStateOf<Boolean?>(null) }
    var showTax by remember { mutableStateOf(false) }

    val state = rememberSales(retry)
    val sales = (state as? SalesLoad.Ready)?.sales.orEmpty()
    val from = fromDay?.let(LocalDate::ofEpochDay)
    val to = toDay?.let(LocalDate::ofEpochDay)
    val catalog = remember(sales) { SalesProducts(sales) }
    val report = remember(sales, from, to, product, period) { salesReport(sales, from, to, product, period, catalog) }

    val orders = (state as? SalesLoad.Ready)?.orders.orEmpty()
    val orderStatistics = remember(orders, from, to, period) { orderReport(orders, from, to, period) }

    val barProgress = remember { Animatable(0f) }
    LaunchedEffect(report.products) {
        barProgress.snapTo(0f)
        barProgress.animateTo(1f, tween(480))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.admin_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state is SalesLoad.Ready) item { SalesTotal(report.total) }
            item {
                SalesPanel {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selectStart = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.sales_start, from?.format(dateFormat) ?: "—"))
                        }
                        OutlinedButton(onClick = { selectStart = false }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.sales_end, to?.format(dateFormat) ?: "—"))
                        }
                    }
                    ProductFilter(catalog.choices, product, onProductChange = { product = it })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { fromDay = null; toDay = null; product = "" }) {
                            Text(stringResource(R.string.sales_clear))
                        }
                        TextButton(onClick = { showTax = true }) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sales_tax_setup))
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalesPeriod.entries.forEach { option ->
                            val label = when (option) {
                                SalesPeriod.Day -> R.string.sales_day
                                SalesPeriod.Week -> R.string.sales_week
                                SalesPeriod.Month -> R.string.sales_month
                            }
                            FilterChip(selected = period == option, onClick = { period = option },
                                label = { Text(stringResource(label)) })
                        }
                    }
                }
            }
            when (state) {
                SalesLoad.Loading -> item { SalesPanel { LinearProgressIndicator(Modifier.fillMaxWidth()) } }
                SalesLoad.Error -> item {
                    Text(stringResource(R.string.sales_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text(stringResource(R.string.sales_retry)) }
                }
                is SalesLoad.Ready -> {
                    item { OrderStatistics(orderStatistics, dateFormat, locale) }
                    if (report.timeline.isEmpty()) item { SalesPanel { Text(stringResource(R.string.sales_empty)) } }
                    else {
                        item {
                            SalesPanel {
                                Text(stringResource(R.string.sales_over_time), style = MaterialTheme.typography.titleLarge)
                                TimelineChart(report.timeline, dateFormat)
                            }
                        }
                        item { Text(stringResource(R.string.sales_top_products), style = MaterialTheme.typography.titleLarge) }
                        if (report.products.isEmpty()) item { Text(stringResource(R.string.sales_no_lines)) }
                        val maxUnits = report.products.maxOfOrNull { it.units } ?: 1L
                        items(report.products) { entry ->
                            SalesPanel {
                                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.sales_product_amount, entry.units, entry.cents.mxn()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val colors = MaterialTheme.colorScheme
                                val fraction = (entry.units.toDouble() / maxUnits).toFloat()
                                Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                                    val radius = CornerRadius(size.height / 2)
                                    drawRoundRect(colors.surfaceVariant, cornerRadius = radius)
                                    val width = size.width * fraction * barProgress.value
                                    if (width > 0f) {
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(listOf(colors.primary, colors.secondary)),
                                            size = size.copy(width = width),
                                            cornerRadius = radius
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectStart?.let { start ->
        val selected = if (start) fromDay else toDay
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = selected?.times(MILLIS_PER_DAY),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Math.floorDiv(utcTimeMillis, MILLIS_PER_DAY)
                    return if (start) toDay?.let { day <= it } ?: true
                    else fromDay?.let { day >= it } ?: true
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { selectStart = null },
            confirmButton = {
                TextButton(enabled = dateState.selectedDateMillis != null, onClick = {
                    val day = dateState.selectedDateMillis?.let { Math.floorDiv(it, MILLIS_PER_DAY) }
                    if (start) fromDay = day else toDay = day
                    selectStart = null
                }) { Text(stringResource(R.string.sales_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { selectStart = null }) { Text(stringResource(R.string.admin_cancel)) }
            }
        ) { DatePicker(dateState) }
    }
    if (showTax) SalesTaxDialog(onClose = { showTax = false })
}

@Composable
private fun SalesTotal(total: Long) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer
    ) {
        Row(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(colors.primaryContainer, colors.secondary.copy(alpha = 0.16f)))
            ).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.admin_total_sales), style = MaterialTheme.typography.titleMedium)
                Crossfade(targetState = total, animationSpec = tween(220), label = "sales total") { value ->
                    Text(
                        stringResource(R.string.sales_mxn, value.mxn()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFilter(products: List<SalesProduct>, selected: String, onProductChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val all = stringResource(R.string.sales_all_products)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = products.firstOrNull { it.key == selected }?.name ?: all, onValueChange = {}, readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text(stringResource(R.string.sales_product_filter)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf(SalesProduct("", all)) + products).forEach { product ->
                DropdownMenuItem(text = { Text(product.name) }, onClick = {
                    onProductChange(product.key)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun OrderStatistics(report: OrderReport, format: DateTimeFormatter, locale: java.util.Locale) {
    val numbers = remember(locale) { java.text.NumberFormat.getIntegerInstance(locale) }
    SalesPanel {
        Text(stringResource(R.string.order_stats_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.order_stats_scope), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (report.timeline.isEmpty()) {
            Text(stringResource(R.string.order_stats_empty))
        } else {
            Text(stringResource(R.string.order_stats_timeline), style = MaterialTheme.typography.titleMedium)
            TimelineChart(report.timeline, format) { numbers.format(it) }
            Text(stringResource(R.string.order_stats_status), style = MaterialTheme.typography.titleMedium)
            val maximum = report.statuses.values.maxOrNull() ?: 1
            val scheme = MaterialTheme.colorScheme
            for (status in listOf("ESPERANDO PAGO", "PENDIENTE", "ENVIADO", "ENTREGADO", "")) {
                val count = report.statuses[status] ?: 0
                if (status.isEmpty() && count == 0) continue
                val label = if (status.isEmpty()) stringResource(R.string.order_stats_other)
                    else com.developers.core.text.orderStatusLabel(status)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label)
                    Text(numbers.format(count))
                }
                val fraction = count.toFloat() / maximum
                val animated by androidx.compose.animation.core.animateFloatAsState(fraction, label = "orderStatus")
                Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(scheme.surfaceVariant, cornerRadius = radius)
                    if (animated > 0f) {
                        drawRoundRect(scheme.primary, size = size.copy(width = size.width * animated), cornerRadius = radius)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineChart(
    points: List<Pair<LocalDate, Long>>,
    format: DateTimeFormatter,
    formatValue: (Long) -> String = { it.mxn() }
) {
    val color = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    val low = minOf(0L, points.minOf { it.second }).toDouble()
    val high = maxOf(1L, points.maxOf { it.second }).toDouble()
    val first = points.first().first.toEpochDay()
    val span = (points.last().first.toEpochDay() - first).coerceAtLeast(1)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(600))
    }
    Canvas(Modifier.fillMaxWidth().height(170.dp).padding(vertical = 10.dp, horizontal = 6.dp)) {
        fun y(amount: Double) = size.height * ((high - amount) / (high - low)).toFloat()
        for (step in 0..4) {
            val gridY = size.height * step / 4
            drawLine(axis.copy(alpha = 0.22f), Offset(0f, gridY), Offset(size.width, gridY))
        }
        val zero = y(0.0)
        drawLine(axis.copy(alpha = 0.55f), Offset(0f, zero), Offset(size.width, zero))
        val reveal = progress.value
        val positions = points.map { (date, amount) ->
            val x = if (points.size == 1) size.width / 2 else
                size.width * ((date.toEpochDay() - first).toDouble() / span).toFloat()
            Offset(x, y(amount.toDouble() * reveal))
        }
        if (positions.size > 1) {
            val line = Path().apply {
                moveTo(positions.first().x, positions.first().y)
                positions.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val area = Path().apply {
                moveTo(positions.first().x, positions.first().y)
                positions.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(positions.last().x, zero)
                lineTo(positions.first().x, zero)
                close()
            }
            drawPath(area, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.06f))))
            drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        if (positions.size <= 24) positions.forEach { drawCircle(color, radius = 3.dp.toPx(), center = it) }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(points) { (date, amount) ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(date.format(format), style = MaterialTheme.typography.labelSmall)
                    Text(formatValue(amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SalesTaxDialog(onClose: () -> Unit) {
    val document = remember { salesTaxDocument(FirebaseFirestore.getInstance()) }
    var rate by remember { mutableStateOf("") }
    var included by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(retry) {
        document.get(Source.SERVER).addOnCompleteListener { task ->
            val settings = (if (task.isSuccessful) task.result else null)?.let { snapshot ->
                runCatching { readSalesTax(snapshot.data) }.getOrNull()
            }
            if (settings == null) error = true
            else {
                rate = settings.percent.toString()
                included = settings.included
                loaded = true
            }
        }
    }
    val percent = rate.trim().replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = { if (!saving) onClose() },
        title = { Text(stringResource(R.string.sales_tax_setup)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!loaded && !error || saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error) {
                    Text(stringResource(R.string.sales_tax_error), color = MaterialTheme.colorScheme.error)
                    if (!loaded) TextButton(onClick = { error = false; retry++ }) {
                        Text(stringResource(R.string.sales_retry))
                    }
                }
                if (loaded) {
                    OutlinedTextField(rate, { rate = it }, enabled = !saving, singleLine = true,
                        label = { Text(stringResource(R.string.sales_tax_rate)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(included, { included = it }, enabled = !saving)
                        Text(stringResource(R.string.sales_tax_included))
                    }
                    Text(stringResource(if (included) R.string.sales_tax_included_note else R.string.sales_tax_added_note))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = loaded && !saving && percent != null && percent in 0.0..100.0, onClick = {
                val value = percent ?: return@TextButton
                saving = true
                error = false
                document.set(mapOf("porcentaje" to value, "incluido" to included))
                    .addOnSuccessListener { onClose() }
                    .addOnFailureListener { saving = false; error = true }
            }) { Text(stringResource(R.string.sales_save)) }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onClose) { Text(stringResource(R.string.admin_cancel)) }
        }
    )
}
