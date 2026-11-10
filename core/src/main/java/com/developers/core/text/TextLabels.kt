package com.developers.core.text

import com.developers.core.R

// Keep persisted values intact. Only map the finite codes defined by the local UI.
// Unknown values are returned verbatim.
fun paymentMethodLabel(value: String): String = when (value) {
    "Tarjeta", "TARJETA" -> localizedText(R.string.pan_payment_card)
    "Transferencia" -> localizedText(R.string.pan_payment_transfer)
    "Efectivo" -> localizedText(R.string.pan_payment_cash)
    "Pago Local" -> localizedText(R.string.pan_payment_local)
    else -> value
}

fun attendanceLabel(value: String): String = when (value) {
    "ENTRADA" -> localizedText(R.string.pan_entry)
    "SALIDA" -> localizedText(R.string.pan_exit)
    else -> value
}

fun orderStatusLabel(value: String): String = when (value) {
    "PENDIENTE" -> localizedText(R.string.pan_status_pending)
    "ENVIADO" -> localizedText(R.string.pan_status_shipped)
    "ENTREGADO" -> localizedText(R.string.pan_status_delivered)
    else -> value
}

fun stockStatusLabel(value: String): String = when (value) {
    "AGOTADO" -> localizedText(R.string.pan_status_out)
    "Crítico" -> localizedText(R.string.pan_status_critical)
    "Óptimo" -> localizedText(R.string.pan_status_optimal)
    else -> value
}

fun categoryLabel(value: String): String = when (value) {
    "Todo", "TODOS", "TODO" -> localizedText(R.string.pan_category_all)
    "PANES" -> localizedText(R.string.pan_category_breads)
    "CAFÉ" -> localizedText(R.string.pan_category_coffee)
    "OTROS", "Otros" -> localizedText(R.string.pan_category_other)
    "Harinas" -> localizedText(R.string.pan_category_flour)
    "Lácteos" -> localizedText(R.string.pan_category_dairy)
    "Azúcares" -> localizedText(R.string.pan_category_sugar)
    "Huevos" -> localizedText(R.string.pan_category_eggs)
    else -> value
}
