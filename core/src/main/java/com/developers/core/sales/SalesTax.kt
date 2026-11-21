package com.developers.core.sales

import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode

data class SalesTax(val percent: Double = 8.0, val included: Boolean = false) {
    fun quote(lines: List<Double>, delivery: Double): SaleQuote {
        val rate = BigDecimal.valueOf(percent).movePointLeft(2)
        val amounts = lines.map { money(BigDecimal.valueOf(it)) }
        val base = amounts.fold(BigDecimal.ZERO, BigDecimal::add)
        val tax = money(if (included) base.subtract(base.divide(BigDecimal.ONE + rate, 10, RoundingMode.HALF_UP)) else base * rate)
        var cumulative = BigDecimal.ZERO
        var allocated = BigDecimal.ZERO
        val lineTotals = amounts.map { amount ->
            cumulative += amount
            val next = if (base.signum() == 0) BigDecimal.ZERO else (tax * cumulative).divide(base, 2, RoundingMode.HALF_UP)
            val lineTax = next - allocated
            allocated = next
            (if (included) amount else amount + lineTax).toDouble()
        }
        val shipping = money(BigDecimal.valueOf(delivery))
        return SaleQuote(
            (if (included) base - tax else base).toDouble(),
            tax.toDouble(), shipping.toDouble(),
            ((if (included) base else base + tax) + shipping).toDouble(), lineTotals
        )
    }

    private fun money(value: BigDecimal) = value.setScale(2, RoundingMode.HALF_UP)
}

data class SaleQuote(val subtotal: Double, val tax: Double, val delivery: Double, val total: Double, val lineTotals: List<Double>)

fun salesTaxDocument(db: FirebaseFirestore) = db.collection("configuracion").document("iva")

fun readSalesTax(data: Map<String, Any>?): SalesTax {
    if (data == null) return SalesTax()
    val percent = (data["porcentaje"] as? Number)?.toDouble()
    val included = data["incluido"] as? Boolean
    require(percent != null && percent in 0.0..100.0 && included != null)
    return SalesTax(percent, included)
}
