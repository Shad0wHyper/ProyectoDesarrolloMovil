package com.developers.admin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Composable
private fun businessDate(): LocalDate = produceState(LocalDate.now(salesZone)) {
    while (true) {
        val now = ZonedDateTime.now(salesZone)
        value = now.toLocalDate()
        val next = value.plusDays(1).atStartOfDay(salesZone)
        delay(Duration.between(now, next).toMillis().coerceIn(1, 60_000))
    }
}.value


@Composable
internal fun WarehouseSummaryCard(state: WarehouseLoad, modifier: Modifier, onClick: () -> Unit) {
    val today = businessDate()
    val count = (state as? WarehouseLoad.Ready)?.items?.count { it.alerts(today).isNotEmpty() }
    val hasAlerts = (count ?: 0) > 0
    val (alertBackground, alertContent) = warehouseAlertColors(WarehouseAlert.Low)
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAlerts) alertBackground else colors.primaryContainer,
            contentColor = if (hasAlerts) alertContent else colors.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (hasAlerts) Icons.Default.Warning else Icons.Default.Inventory2, contentDescription = null)
            Text(stringResource(R.string.warehouse_title), style = MaterialTheme.typography.titleMedium)
            when (state) {
                WarehouseLoad.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                WarehouseLoad.Error -> Text(stringResource(R.string.warehouse_error))
                is WarehouseLoad.Ready -> {
                    val count = state.items.count { it.alerts(today).isNotEmpty() }
                    Text(if (count == 0) stringResource(R.string.warehouse_no_alerts)
                    else pluralStringResource(R.plurals.warehouse_attention_count, count, count))
                }
            }
        }
    }
}

private val WarehouseAlert.title: Int get() = when (this) {
    WarehouseAlert.Expired -> R.string.warehouse_expired
    WarehouseAlert.Out -> R.string.warehouse_out
    WarehouseAlert.Expiring -> R.string.warehouse_expiring
    WarehouseAlert.Low -> R.string.warehouse_low
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WarehouseStatusScreen(
    state: WarehouseLoad,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onControl: (String?) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var all by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<WarehouseAlert?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val today = businessDate()
    val inventory = (state as? WarehouseLoad.Ready)?.items.orEmpty()
    val alerts = remember(inventory, today) { inventory.associate { it.material.id to it.alerts(today) } }
    val counts = remember(alerts) {
        WarehouseAlert.entries.associateWith { alert -> alerts.values.count { alert in it } }
    }
    val shown = remember(inventory, alerts, search, all, filter) {
        inventory.filter { entry ->
            val flags = alerts.getValue(entry.material.id)
            entry.material.nombre.contains(search.trim(), ignoreCase = true) &&
                (filter?.let { it in flags } ?: (all || flags.isNotEmpty()))
        }.sortedWith(compareBy<WarehouseItem> {
            alerts.getValue(it.material.id).firstOrNull()?.ordinal ?: Int.MAX_VALUE
        }.thenBy { it.material.nombre.lowercase() })
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.warehouse_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.admin_back))
                }
            }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listOf(
                listOf(WarehouseAlert.Out, WarehouseAlert.Low),
                listOf(WarehouseAlert.Expiring, WarehouseAlert.Expired)
            )) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { alert ->
                        val active = filter == alert
                        val (background, foreground) = warehouseAlertColors(alert)
                        val outline by animateColorAsState(
                            if (active) foreground else Color.Transparent,
                            label = "warehouse filter selection"
                        )
                        Card(
                            onClick = { filter = if (active) null else alert },
                            modifier = Modifier.weight(1f).semantics { selected = active },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(2.dp, outline),
                            colors = CardDefaults.cardColors(containerColor = background, contentColor = foreground)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(warehouseAlertIcon(alert), contentDescription = null, modifier = Modifier.size(20.dp))
                                    if (active) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                                Crossfade(
                                    targetState = if (state is WarehouseLoad.Ready) counts.getValue(alert).toString() else "—",
                                    animationSpec = tween(220),
                                    label = "warehouse count"
                                ) { value ->
                                    Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(stringResource(alert.title), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = { onControl(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Inventory2, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.warehouse_control))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !all, onClick = { all = false },
                        label = { Text(stringResource(R.string.warehouse_attention)) })
                    FilterChip(selected = all, onClick = { all = true },
                        label = { Text(stringResource(R.string.warehouse_all)) })
                }
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.warehouse_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                TextButton(onClick = { search = ""; all = false; filter = null }) {
                    Text(stringResource(R.string.warehouse_clear))
                }
            }
            when (state) {
                WarehouseLoad.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                WarehouseLoad.Error -> item {
                    Text(stringResource(R.string.warehouse_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.warehouse_retry)) }
                }
                is WarehouseLoad.Ready -> {
                    if (shown.isEmpty()) item {
                        val message = when {
                            inventory.isEmpty() -> R.string.warehouse_empty
                            search.isNotBlank() || filter != null -> R.string.warehouse_no_matches
                            else -> R.string.warehouse_no_alerts
                        }
                        Text(stringResource(message))
                    }
                    items(shown, key = { it.material.id }) { entry ->
                        Card(
                            onClick = { selectedId = entry.material.id },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.material.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(stockLabel(entry.material.cantidadActual, entry.material.unidadMedida))
                                WarehouseFlags(entry, today)
                            }
                        }
                    }
                }
            }
        }
    }

    inventory.firstOrNull { it.material.id == selectedId }?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedId = null },
            title = { Text(entry.material.nombre) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val unit = entry.material.unidadMedida
                    Text(stringResource(R.string.warehouse_stock, stockLabel(entry.material.cantidadActual, unit)))
                    Text(entry.minimum?.let { stringResource(R.string.warehouse_minimum, stockLabel(it, unit)) }
                        ?: stringResource(R.string.warehouse_no_minimum))
                    WarehouseFlags(entry, today, allDates = true)
                    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(LocalConfiguration.current.locales[0])
                    if (entry.lots.isEmpty()) Text(stringResource(R.string.warehouse_no_date))
                    else entry.lots.forEach { lot ->
                        Text(stringResource(R.string.warehouse_lot, stockLabel(lot.remaining, unit),
                            lot.expires?.format(formatter) ?: stringResource(R.string.warehouse_no_date)))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onControl(entry.material.id); selectedId = null }) {
                    Text(stringResource(R.string.warehouse_entry))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.admin_cancel)) }
            }
        )
    }
}

@Composable
private fun stockLabel(amount: Double, unit: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 6 }.format(amount)
    return "$number $unit".trimEnd()
}

private fun warehouseAlertIcon(alert: WarehouseAlert) = when (alert) {
    WarehouseAlert.Out -> Icons.Default.ErrorOutline
    WarehouseAlert.Low -> Icons.Default.Warning
    WarehouseAlert.Expiring -> Icons.Default.Schedule
    WarehouseAlert.Expired -> Icons.Default.EventBusy
}

@Composable
private fun warehouseAlertColors(alert: WarehouseAlert): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (alert) {
        WarehouseAlert.Out -> if (dark) Color(0xFF4E2028) to Color(0xFFFFB4BD) else Color(0xFFFFE2E6) to Color(0xFF8B1830)
        WarehouseAlert.Low -> if (dark) Color(0xFF512822) to Color(0xFFFFB5A7) else Color(0xFFFFE4DF) to Color(0xFF962C20)
        WarehouseAlert.Expiring -> if (dark) Color(0xFF473314) to Color(0xFFFFCB78) else Color(0xFFFFEDCE) to Color(0xFF754800)
        WarehouseAlert.Expired -> if (dark) Color(0xFF4B2641) to Color(0xFFFAB3DE) else Color(0xFFFCE0F1) to Color(0xFF81245C)
    }
}

@Composable
private fun WarehouseBadge(alert: WarehouseAlert, label: String) {
    val (background, foreground) = warehouseAlertColors(alert)
    Surface(shape = RoundedCornerShape(10.dp), color = background, contentColor = foreground) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(warehouseAlertIcon(alert), contentDescription = null, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun WarehouseFlags(entry: WarehouseItem, today: LocalDate, allDates: Boolean = false) {
    entry.alerts(today).forEach { alert ->
        val dates = entry.expiryDates(today, alert)
        if (dates.isEmpty()) {
            WarehouseBadge(alert, stringResource(if (alert == WarehouseAlert.Out) R.string.warehouse_out_single else alert.title))
        } else {
            (if (allDates) dates else dates.take(1)).forEach { date ->
                val days = ChronoUnit.DAYS.between(today, date).toInt()
                val label = when {
                    days == 0 -> stringResource(R.string.warehouse_expires_today)
                    days == -1 -> stringResource(R.string.warehouse_expired_yesterday)
                    days > 0 -> pluralStringResource(R.plurals.warehouse_expires_days, days, days)
                    else -> pluralStringResource(R.plurals.warehouse_expired_days, -days, -days)
                }
                WarehouseBadge(alert, label)
            }
        }
    }
}
