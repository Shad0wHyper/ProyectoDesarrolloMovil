package com.developers.employee

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class OrderStatusTest {

    @Test
    fun `normalizeOrderStatus handles canonical and legacy variants`() {
        assertEquals("PENDIENTE", normalizeOrderStatus("PENDIENTE", null))
        assertEquals("PENDIENTE", normalizeOrderStatus(null, "pendiente"))
        assertEquals("PENDIENTE", normalizeOrderStatus("Procesando", null))
        assertEquals("PENDIENTE", normalizeOrderStatus("Procesados", null))
        assertEquals("PENDIENTE", normalizeOrderStatus("En preparación", null))
        assertEquals("PENDIENTE", normalizeOrderStatus("   ", null))

        assertEquals("ENVIADO", normalizeOrderStatus("ENVIADO", null))
        assertEquals("ENVIADO", normalizeOrderStatus("en camino", null))
        assertEquals("ENVIADO", normalizeOrderStatus("EN_CAMINO", null))
        assertEquals("ENVIADO", normalizeOrderStatus("On the way", null))
        assertEquals("ENVIADO", normalizeOrderStatus(null, "En tránsito"))

        assertEquals("ENTREGADO", normalizeOrderStatus("ENTREGADO", null))
        assertEquals("ENTREGADO", normalizeOrderStatus("entregados", null))
        assertEquals("ENTREGADO", normalizeOrderStatus("COMPLETADO", null))
        assertEquals("ENTREGADO", normalizeOrderStatus("Delivered", null))
        assertEquals("ENTREGADO", normalizeOrderStatus("CERRADO - ENTREGADO", null))

        assertEquals("ESPERANDO PAGO", normalizeOrderStatus("ESPERANDO PAGO", null))
        assertEquals("ESPERANDO PAGO", normalizeOrderStatus(null, "esperando_pago"))
    }

    @Test
    fun `orderDispatchFilter maps all active orders into one of the three filter buckets`() {
        assertEquals("PENDIENTE", orderDispatchFilter("PENDIENTE"))
        assertEquals("PENDIENTE", orderDispatchFilter("ESPERANDO PAGO"))
        assertEquals("PENDIENTE", orderDispatchFilter("Procesado"))
        assertEquals("PENDIENTE", orderDispatchFilter("DESCONOCIDO"))

        assertEquals("ENVIADO", orderDispatchFilter("ENVIADO"))
        assertEquals("ENVIADO", orderDispatchFilter("En Camino"))

        assertEquals("ENTREGADO", orderDispatchFilter("ENTREGADO"))
        assertEquals("ENTREGADO", orderDispatchFilter("COMPLETADO"))
    }

    @Test
    fun `parseOrderTimestampMillis parses Timestamp, Number, and String`() {
        val millis = 1_764_000_000_000L
        assertEquals(millis, parseOrderTimestampMillis(Timestamp(Date(millis)), null))
        assertEquals(millis, parseOrderTimestampMillis(millis, null))
        assertEquals(millis, parseOrderTimestampMillis(null, millis.toString()))
        assertEquals(0L, parseOrderTimestampMillis(null, "invalid"))
    }
}
