package com.example.smartbudget_asistent_financiar.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ReceiptList : Screen("receipts")
    object Scan : Screen("scan")
    object ReceiptDetail : Screen("receipt/{receiptId}") {
        fun createRoute(receiptId: Long) = "receipt/$receiptId"
    }
}
