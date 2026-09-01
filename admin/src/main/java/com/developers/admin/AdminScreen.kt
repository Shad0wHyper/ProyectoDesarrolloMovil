package com.developers.admin

sealed class AdminScreen(val route: String) {
    object Dashboard : AdminScreen("dashboard")
    object Almacen : AdminScreen("almacen")
    object Pedidos : AdminScreen("pedidos")
    object IAReport : AdminScreen("ia_report")
    object AddProduct : AdminScreen("add_product")
    object QrGenerator : AdminScreen("qr_generator")
}
