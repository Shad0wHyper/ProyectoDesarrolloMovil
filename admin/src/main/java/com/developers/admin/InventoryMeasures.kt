package com.developers.admin

import java.math.BigDecimal
import java.math.RoundingMode

internal enum class InventoryUnit(val symbol: String, val group: String, val factor: String) {
    Milligram("mg", "mass", "0.001"),
    Gram("g", "mass", "1"),
    Kilogram("kg", "mass", "1000"),
    Milliliter("ml", "volume", "1"),
    Centiliter("cl", "volume", "10"),
    Liter("L", "volume", "1000"),
    Piece("pz", "count", "1"),
    Dozen("doc", "count", "12");

    companion object {
        fun from(symbol: String) = entries.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
    }
}

internal fun inputNumber(text: String): BigDecimal? =
    text.trim().replace(',', '.').takeIf { it.matches(Regex("[0-9]{1,12}([.][0-9]{1,6})?")) }
        ?.toBigDecimalOrNull()

internal fun convertInventoryAmount(amount: BigDecimal, from: String, to: String): Double? {
    if (amount.signum() <= 0) return null
    if (from == to) return amount.toDouble()
    val source = InventoryUnit.from(from) ?: return null
    val target = InventoryUnit.from(to) ?: return null
    if (source.group != target.group) return null
    return (amount * source.factor.toBigDecimal())
        .divide(target.factor.toBigDecimal(), 12, RoundingMode.HALF_UP)
        .toDouble().takeIf { it > 0 }
}

internal fun suggestedRestock(stock: BigDecimal, target: BigDecimal, packageSize: BigDecimal?): BigDecimal {
    val missing = (target - stock).max(BigDecimal.ZERO)
    return if (packageSize == null) missing
    else missing.divide(packageSize, 0, RoundingMode.CEILING) * packageSize
}

internal fun BigDecimal.inventoryInput(): String = stripTrailingZeros().toPlainString()
