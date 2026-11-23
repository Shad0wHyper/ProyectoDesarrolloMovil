package com.developers.admin

import com.developers.core.ui.PanAppScreenTitle

import com.developers.core.ui.PanAppGlassCard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
internal fun WeeklySalesCard(state: SalesLoad, onClick: () -> Unit) {
    PanAppGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        WeeklySalesContent(state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeeklySalesScreen(onBack: () -> Unit) {
    var retry by remember { mutableIntStateOf(0) }
    val state = rememberSales(retry)
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),            title = { PanAppScreenTitle(stringResource(R.string.weekly_sales_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.admin_back))
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            PanAppGlassCard(
                shape = MaterialTheme.shapes.large
            ) {
                WeeklySalesContent(state, expanded = true, onRetry = { retry++ })
            }
        }
    }
}

@Composable
private fun WeeklySalesContent(state: SalesLoad, expanded: Boolean = false, onRetry: (() -> Unit)? = null) {
    val start = SalesPeriod.Week.start(LocalDate.now(salesZone))
    val dates = remember(start) { List(7) { start.plusDays(it.toLong()) } }
    val sales = (state as? SalesLoad.Ready)?.sales
    val week = remember(sales, start) { sales.orEmpty().filter { it.date in start..dates.last() } }
    val locale = LocalConfiguration.current.locales[0]
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.BarChart, contentDescription = null, tint = colors.primary)
            Text(
                stringResource(R.string.weekly_sales_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            if (state is SalesLoad.Ready) {
                Surface(shape = MaterialTheme.shapes.small, color = colors.primaryContainer) {
                    Text(
                        pluralStringResource(R.plurals.weekly_sales_paid_orders, week.size, week.size),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        if (expanded) {
            val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
            Text(
                "${start.format(format)} – ${dates.last().format(format)} · MXN",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        when (state) {
            SalesLoad.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            SalesLoad.Error -> {
                Text(stringResource(R.string.sales_error), color = colors.error)
                if (onRetry != null) TextButton(onClick = onRetry) { Text(stringResource(R.string.sales_retry)) }
            }
            is SalesLoad.Ready -> {
                val amounts = remember(week, dates) {
                    val salesByDay = week.groupBy { it.date }
                    dates.map { date -> salesByDay[date].orEmpty().sumOf { it.total } }
                }
                val high = amounts.max().coerceAtLeast(1L)
                val low = amounts.min().coerceAtMost(0L)
                val chartHeight = if (expanded) 230.dp else 114.dp
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.height(chartHeight), verticalArrangement = Arrangement.SpaceBetween) {
                        Text(high.mxn(), style = MaterialTheme.typography.labelSmall)
                        Text(low.mxn(), style = MaterialTheme.typography.labelSmall)
                    }
                    dates.forEachIndexed { index, date ->
                        val amount = amounts[index]
                        val day = date.dayOfWeek
                        Column(
                            Modifier.weight(1f).clearAndSetSemantics {
                                contentDescription = "${day.getDisplayName(TextStyle.FULL, locale)}, ${amount.mxn()}"
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
                                fun y(value: Long) = ((high.toDouble() - value) / (high.toDouble() - low)).toFloat() * size.height
                                val zero = y(0L)
                                for (step in 0..2) {
                                    val guide = size.height * step / 3f
                                    drawLine(
                                        colors.outlineVariant,
                                        Offset(0f, guide), Offset(size.width, guide),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                                    )
                                }
                                drawLine(colors.outlineVariant, Offset(0f, zero), Offset(size.width, zero))
                                if (amount != 0L) {
                                    val top = minOf(y(amount), zero)
                                    val bottom = maxOf(y(amount), zero)
                                    val width = size.width * 0.74f
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(listOf(Color(0xFFFFB95C), Color(0xFF733BE5)), top, bottom),
                                        topLeft = Offset((size.width - width) / 2, top),
                                        size = Size(width, bottom - top),
                                        cornerRadius = CornerRadius(4.dp.toPx())
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                day.getDisplayName(TextStyle.SHORT, locale),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (week.isEmpty()) Text(stringResource(R.string.sales_empty), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
