package com.developers.employee

import com.google.firebase.Timestamp
import java.text.Normalizer
import java.util.Locale

private val DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val MULTI_SPACE_REGEX = Regex("\\s+")

private fun cleanOrderToken(value: String): String {
    val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    return DIACRITICS_REGEX.replace(decomposed, "")
        .replace(MULTI_SPACE_REGEX, " ")
        .uppercase(Locale.ROOT)
}

internal fun normalizeOrderStatus(rawEstado: String?, rawStatus: String?): String {
    val raw = listOfNotNull(rawEstado, rawStatus)
        .firstOrNull { it.isNotBlank() }
        ?: return "PENDIENTE"

    return when (cleanOrderToken(raw)) {
        "ENTREGADO", "ENTREGADOS", "DELIVERED", "COMPLETADO", "COMPLETADOS", "COMPLETED",
        "CERRADO", "CERRADO - ENTREGADO", "ENTREGUE", "ENTREGUES" -> "ENTREGADO"

        "ENVIADO", "ENVIADOS", "EN CAMINO", "EN_CAMINO", "ON THE WAY", "ON_THE_WAY",
        "SHIPPED", "EN TRANSITO", "IN TRANSIT", "A CAMINHO" -> "ENVIADO"

        "ESPERANDO PAGO", "ESPERANDO_PAGO", "AWAITING PAYMENT", "PAGO PENDIENTE" -> "ESPERANDO PAGO"

        "PENDIENTE", "PENDIENTES", "PENDING", "PROCESADO", "PROCESADOS", "PROCESANDO",
        "PROCESSED", "EN PROCESO", "EN PREPARACION", "PREPARANDO" -> "PENDIENTE"

        else -> raw.trim().uppercase(Locale.ROOT)
    }
}

internal fun orderDispatchFilter(estado: String): String = when (normalizeOrderStatus(estado, null)) {
    "ENTREGADO" -> "ENTREGADO"
    "ENVIADO" -> "ENVIADO"
    else -> "PENDIENTE"
}

internal fun parseOrderTimestampMillis(rawTimestamp: Any?, rawFecha: Any?): Long =
    parseTimestampCandidate(rawTimestamp) ?: parseTimestampCandidate(rawFecha) ?: 0L

private fun parseTimestampCandidate(value: Any?): Long? = when (value) {
    is Timestamp -> value.toDate().time
    is Number -> value.toLong()
    is String -> value.trim().toLongOrNull()
    else -> null
}
