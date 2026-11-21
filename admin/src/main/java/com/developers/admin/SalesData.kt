package com.developers.admin

import androidx.compose.runtime.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Currency
import java.util.Locale

internal val salesZone = ZoneId.of("America/Mexico_City")

internal fun Long.mxn(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
    .apply { currency = Currency.getInstance("MXN") }
    .format(BigDecimal.valueOf(this, 2))

private fun Number.cents(): Long = BigDecimal(toString()).movePointRight(2)
    .setScale(0, RoundingMode.HALF_UP).longValueExact()

internal data class SaleLine(val productId: String?, val name: String, val units: Long, val cents: Long)
internal data class Sale(val date: LocalDate, val total: Long, val lines: List<SaleLine>)
internal data class SalesProduct(val key: String, val name: String)

internal class SalesProducts(sales: List<Sale>) {
    private val lines = sales.flatMap { it.lines }
    private val idsByName = lines.groupBy { it.name }.mapValues { (_, items) ->
        items.mapNotNull { it.productId }.distinct()
    }

    fun key(line: SaleLine): String {
        val id = line.productId ?: idsByName[line.name]?.singleOrNull()
        return if (id == null) "name:${line.name}" else "id:$id"
    }

    val choices = lines.map { SalesProduct(key(it), it.name) }.distinctBy { it.key }.sortedBy { it.name }
}

internal data class ProductSale(val name: String, val units: Long, val cents: Long)
internal data class SalesReport(val total: Long, val timeline: List<Pair<LocalDate, Long>>, val products: List<ProductSale>)
internal enum class SalesPeriod {
    Day, Week, Month;

    fun start(date: LocalDate): LocalDate = when (this) {
        Day -> date
        Week -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Month -> date.withDayOfMonth(1)
    }
}

internal fun salesReport(sales: List<Sale>, from: LocalDate?, to: LocalDate?, product: String, period: SalesPeriod, catalog: SalesProducts = SalesProducts(sales)): SalesReport {
    val timeline = sortedMapOf<LocalDate, Long>()
    val products = mutableMapOf<String, ProductSale>()
    var total = 0L
    for (sale in sales) {
        if (from != null && sale.date < from || to != null && sale.date > to) continue
        val lines = sale.lines.filter { product.isEmpty() || catalog.key(it) == product }
        if (product.isNotEmpty() && lines.isEmpty()) continue

        val amount = if (product.isEmpty()) sale.total else lines.sumOf { it.cents }
        total += amount
        val bucket = period.start(sale.date)
        timeline[bucket] = timeline.getOrDefault(bucket, 0L) + amount

        for (line in lines) {
            val key = catalog.key(line)
            val current = products[key]
            products[key] = ProductSale(
                current?.name ?: line.name,
                (current?.units ?: 0) + line.units,
                (current?.cents ?: 0) + line.cents
            )
        }
    }
    return SalesReport(total, timeline.toList(), products.values.sortedWith(
        compareByDescending<ProductSale> { it.units }.thenBy { it.name }
    ))
}

internal data class OrderMetric(val date: LocalDate, val status: String)
internal data class OrderReport(val timeline: List<Pair<LocalDate, Long>>, val statuses: Map<String, Int>)

internal fun orderReport(orders: List<OrderMetric>, from: LocalDate?, to: LocalDate?, period: SalesPeriod): OrderReport {
    val included = orders.filter { (from == null || it.date >= from) && (to == null || it.date <= to) }
    val timeline = included.groupingBy { period.start(it.date) }.eachCount()
        .toSortedMap().map { (date, count) -> date to count.toLong() }
    return OrderReport(timeline, included.groupingBy { it.status }.eachCount())
}

private fun DocumentSnapshot.orderMetric(): OrderMetric? {
    val date = orderDate() ?: return null
    val status = ((get("estado") ?: get("status")) as? String).orEmpty().trim().uppercase(Locale.ROOT)
    return OrderMetric(date, when (status) {
        "ESPERANDO PAGO", "PENDIENTE", "ENVIADO", "ENTREGADO" -> status
        else -> ""
    })
}

private fun DocumentSnapshot.orderDate(): LocalDate? {
    val time = when (val value = get("timestamp")) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> (get("fecha") as? Number)?.toLong()
    } ?: return null
    return Instant.ofEpochMilli(time).atZone(salesZone).toLocalDate()
}

private fun DocumentSnapshot.paidSale(): Sale? {
    val total = (get("total") as? Number)?.cents() ?: return null
    val date = orderDate() ?: return null
    val lines = (get("items") as? List<*>)?.mapNotNull { entry ->
        val line = entry as? Map<*, *> ?: return@mapNotNull null
        val name = line["nombre"] as? String ?: return@mapNotNull null
        val quantity = (line["cantidad"] as? Number)?.toLong() ?: return@mapNotNull null
        if (quantity <= 0) return@mapNotNull null
        val amount = (line["importe"] as? Number)?.cents()
            ?: (line["precio"] as? Number)?.let {
                BigDecimal(it.toString()).multiply(BigDecimal.valueOf(quantity)).cents()
            } ?: return@mapNotNull null
        SaleLine(line["productoId"] as? String, name, quantity, amount)
    }.orEmpty()
    return Sale(date, total, lines)
}

internal sealed interface SalesLoad {
    data object Loading : SalesLoad
    data object Error : SalesLoad
    data class Ready(val sales: List<Sale>, val orders: List<OrderMetric>) : SalesLoad
}

@Composable
internal fun rememberSales(retry: Int): SalesLoad {
    var state by remember(retry) { mutableStateOf<SalesLoad>(SalesLoad.Loading) }
    DisposableEffect(retry) {
        val listener = FirebaseFirestore.getInstance().collectionGroup("pedidos")
            .addSnapshotListener { snapshot, error ->
                state = if (error != null || snapshot == null) SalesLoad.Error
                else runCatching {
                    SalesLoad.Ready(
                        snapshot.documents.mapNotNull { it.paidSale() },
                        snapshot.documents.mapNotNull { it.orderMetric() }
                    )
                }
                    .getOrElse { SalesLoad.Error }
            }
        onDispose { listener.remove() }
    }
    return state
}
