package com.developers.core.sales

import org.junit.Assert.assertEquals
import org.junit.Test

class SalesTaxTest {
    @Test
    fun addedTaxAllocatesRoundedCentsAndLeavesDeliveryUntaxed() {
        val sale = SalesTax(16.0, false).quote(listOf(0.03, 0.03, 100.0), 2.0)
        assertEquals(100.06, sale.subtotal, 0.0001)
        assertEquals(16.01, sale.tax, 0.0001)
        assertEquals(118.07, sale.total, 0.0001)
        assertEquals(sale.total - sale.delivery, sale.lineTotals.sum(), 0.0001)
    }

    @Test
    fun includedTaxDoesNotIncreaseThePrice() {
        val sale = SalesTax(16.0, true).quote(listOf(116.0, 58.0), 2.0)
        assertEquals(150.0, sale.subtotal, 0.0001)
        assertEquals(24.0, sale.tax, 0.0001)
        assertEquals(176.0, sale.total, 0.0001)
        assertEquals(listOf(116.0, 58.0), sale.lineTotals)
    }
}
