package com.developers.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryMeasuresTest {
    @Test
    fun convertsOnlyCompatibleUnitsAndAcceptsDecimalComma() {
        val amount = checkNotNull(inputNumber("1,25"))
        assertEquals(1250.0, convertInventoryAmount(amount, "L", "ml")!!, 0.0)
        assertEquals(15.0, convertInventoryAmount(amount, "doc", "pz")!!, 0.0)
        assertNull(convertInventoryAmount(amount, "L", "g"))
    }

    @Test
    fun roundsShortageUpToTheNextPackageAndNeverSuggestsNegativeStock() {
        assertEquals("12", suggestedRestock(5.toBigDecimal(), 14.toBigDecimal(), 4.toBigDecimal()).inventoryInput())
        assertEquals("0", suggestedRestock(15.toBigDecimal(), 14.toBigDecimal(), null).inventoryInput())
    }
}
