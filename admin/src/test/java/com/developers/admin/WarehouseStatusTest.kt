package com.developers.admin

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WarehouseStatusTest {
    private val today = LocalDate.of(2026, 11, 20)

    @Test
    fun expiryIncludesTodayAndSevenDaysAndIgnoresDepletedLots() {
        val item = WarehouseItem(
            MateriaPrima(cantidadActual = 2.0),
            minimum = 2.0,
            lots = listOf(
                WarehouseLot(0.0, today.minusDays(2)),
                WarehouseLot(1.0, today),
                WarehouseLot(1.0, today.plusDays(7)),
                WarehouseLot(2.0, today.plusDays(8))
            )
        )
        assertEquals(listOf(WarehouseAlert.Expiring, WarehouseAlert.Low), item.alerts(today))
        assertEquals(listOf(today, today.plusDays(7)), item.expiryDates(today, WarehouseAlert.Expiring))
    }

    @Test
    fun missingMinimumDoesNotRaiseLowStockAndExpiredHasPriority() {
        val item = WarehouseItem(MateriaPrima(cantidadActual = 2.0), minimum = null)
        assertEquals(emptyList<WarehouseAlert>(), item.alerts(today))
        val depleted = item.copy(material = MateriaPrima(cantidadActual = 0.0), minimum = 0.0)
        assertEquals(listOf(WarehouseAlert.Out), depleted.alerts(today))
        val expired = item.copy(minimum = 2.0, lots = listOf(WarehouseLot(1.0, today.minusDays(1))))
        assertEquals(listOf(WarehouseAlert.Expired, WarehouseAlert.Low), expired.alerts(today))
    }
}
