package com.developers.admin

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesReportTest {
    @Test
    fun orderStatisticsCountEveryStatusWithinTheInclusiveRange() {
        val monday = LocalDate.of(2026, 11, 16)
        val sunday = monday.plusDays(6)
        val orders = listOf(
            OrderMetric(monday.minusDays(1), "ENTREGADO"),
            OrderMetric(monday, "ESPERANDO PAGO"),
            OrderMetric(sunday, "PENDIENTE"),
            OrderMetric(sunday, ""),
            OrderMetric(sunday.plusDays(1), "ENVIADO")
        )
        val report = orderReport(orders, monday, sunday, SalesPeriod.Week)
        assertEquals(listOf(monday to 3L), report.timeline)
        assertEquals(mapOf("ESPERANDO PAGO" to 1, "PENDIENTE" to 1, "" to 1), report.statuses)
    }

    @Test
    fun inclusiveRangeAndGroupingPreserveTotalsAndProductFilterUsesOnlyItsLines() {
        val monday = LocalDate.of(2026, 11, 16)
        val friday = monday.plusDays(4)
        val bread = SaleLine("bread", "Pan", 2, 2320)
        val cake = SaleLine(null, "Pastel", 1, 5000)
        val orders = listOf(
            Sale(monday.minusDays(1), 5000, listOf(cake)),
            Sale(monday, 7520, listOf(bread, cake)),
            Sale(friday, 5200, listOf(cake)),
            Sale(friday.plusDays(1), 5000, listOf(cake))
        )

        val all = salesReport(orders, monday, friday, "", SalesPeriod.Week)
        assertEquals(12720L, all.total)
        assertEquals(listOf(monday to 12720L), all.timeline)
        val filtered = salesReport(orders, monday, friday, "id:bread", SalesPeriod.Month)
        assertEquals(2320L, filtered.total)
        assertEquals(listOf(monday.withDayOfMonth(1) to 2320L), filtered.timeline)
        assertEquals(listOf(ProductSale("Pan", 2, 2320)), filtered.products)
    }
}
