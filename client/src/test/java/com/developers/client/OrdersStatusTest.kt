package com.developers.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OrdersStatusTest {

    @Test
    fun `orderStatus maps pending and awaiting payment variants to Pending`() {
        assertEquals(OrderFilter.Pending, orderStatus("PENDIENTE"))
        assertEquals(OrderFilter.Pending, orderStatus("ESPERANDO PAGO"))
        assertEquals(OrderFilter.Pending, orderStatus("esperando_pago"))
        assertEquals(OrderFilter.Pending, orderStatus("Procesando"))
        assertEquals(OrderFilter.Pending, orderStatus(""))
    }

    @Test
    fun `orderStatus maps shipped and delivered variants`() {
        assertEquals(OrderFilter.OnTheWay, orderStatus("ENVIADO"))
        assertEquals(OrderFilter.OnTheWay, orderStatus("En camino"))
        assertEquals(OrderFilter.OnTheWay, orderStatus("On the way"))

        assertEquals(OrderFilter.Delivered, orderStatus("ENTREGADO"))
        assertEquals(OrderFilter.Delivered, orderStatus("COMPLETADO"))
        assertEquals(OrderFilter.Delivered, orderStatus("Delivered"))
    }

    @Test
    fun `active order filter chips all have non-null labels`() {
        assertNotNull(OrderFilter.Pending.label)
        assertNotNull(OrderFilter.OnTheWay.label)
        assertNotNull(OrderFilter.Delivered.label)
    }
}
