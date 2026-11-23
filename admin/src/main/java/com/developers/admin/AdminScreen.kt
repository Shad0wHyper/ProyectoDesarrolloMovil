package com.developers.admin

sealed class AdminScreen(val route: String) {
    object Dashboard : AdminScreen("dashboard")
    object Sales : AdminScreen("sales")
    object WeeklySales : AdminScreen("weekly_sales")
    object Almacen : AdminScreen("almacen")
    object WarehouseStock : AdminScreen("warehouse_stock")
    object WarehouseStatus : AdminScreen("warehouse_status")
    object Pedidos : AdminScreen("pedidos")
    object IAReport : AdminScreen("ia_report")
    object AddProduct : AdminScreen("add_product")
    object QrGenerator : AdminScreen("qr_generator")
    object Produccion : AdminScreen("produccion")
    object Asistencia : AdminScreen("asistencia")
    object AttendanceHistory : AdminScreen("attendance_history")
    object Profile : AdminScreen("profile")
    object Settings : AdminScreen("settings")
}

