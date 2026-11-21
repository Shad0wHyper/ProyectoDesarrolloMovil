package com.developers.admin

import java.time.LocalDate

internal enum class WarehouseAlert { Expired, Out, Expiring, Low }

internal data class WarehouseLot(val remaining: Double, val expires: LocalDate?)

internal data class WarehouseItem(
    val material: MateriaPrima,
    val minimum: Double?,
    val lots: List<WarehouseLot> = emptyList()
) {
    fun expiryDates(today: LocalDate, alert: WarehouseAlert): List<LocalDate> =
        lots.asSequence().filter { it.remaining > 0 }.mapNotNull { it.expires }
            .filter { date ->
                when (alert) {
                    WarehouseAlert.Expired -> date < today
                    WarehouseAlert.Expiring -> date >= today && date <= today.plusDays(7)
                    else -> false
                }
            }.distinct().sorted().toList()

    fun alerts(today: LocalDate): List<WarehouseAlert> = WarehouseAlert.entries.filter { alert ->
        when (alert) {
            WarehouseAlert.Expired, WarehouseAlert.Expiring -> expiryDates(today, alert).isNotEmpty()
            WarehouseAlert.Out -> material.cantidadActual <= 0
            WarehouseAlert.Low -> material.cantidadActual > 0 &&
                minimum?.let { material.cantidadActual <= it } == true
        }
    }
}

internal sealed interface WarehouseLoad {
    data object Loading : WarehouseLoad
    data object Error : WarehouseLoad
    data class Ready(val items: List<WarehouseItem>) : WarehouseLoad
}
