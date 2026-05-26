package com.example.smartbudget_asistent_financiar.ui.screens.scan

import java.util.Calendar

object ReceiptParser {

    data class ParsedReceipt(
        val merchant: String,
        val amount: Double,
        val category: String,
        val date: Long
    )

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        return ParsedReceipt(
            merchant = extractMerchant(lines),
            amount = extractAmount(lines),
            category = extractCategory(lines),
            date = extractDate(lines) ?: System.currentTimeMillis()
        )
    }

    private fun extractMerchant(lines: List<String>): String =
        lines.firstOrNull { it.length > 2 }?.take(40) ?: "Unknown"

    private fun extractAmount(lines: List<String>): Double {
        val keywords = listOf("total", "subtotal", "amount", "suma", "plata", "de plata", "lei", "ron")
        for (line in lines.reversed()) {
            if (keywords.any { line.lowercase().contains(it) }) {
                val num = extractDecimalNumbers(line).filter { it > 0 && it < 100_000 }.maxOrNull()
                if (num != null) return num
            }
        }
        return lines.flatMap { extractDecimalNumbers(it) }
            .filter { it > 0 && it < 100_000 }
            .maxOrNull() ?: 0.0
    }

    // Only matches numbers with exactly 2 decimal places (e.g. 45,90 or 45.90)
    private fun extractDecimalNumbers(line: String): List<Double> =
        Regex("""(\d{1,6})[,.](\d{2})""").findAll(line)
            .mapNotNull { "${it.groupValues[1]}.${it.groupValues[2]}".toDoubleOrNull() }
            .toList()

    private fun extractCategory(lines: List<String>): String {
        val text = lines.joinToString(" ").lowercase()
        return when {
            containsAny(text, "lidl", "kaufland", "auchan", "mega image", "carrefour", "profi", "penny", "supermarket", "hypermarket") -> "Groceries"
            containsAny(text, "restaurant", "pizza", "burger", "cafe", "cafenea", "bar", "kfc", "mcdonald", "shawarma", "sushi") -> "Food & Dining"
            containsAny(text, "farmacie", "pharmacy", "medic", "doctor", "spital", "clinica", "catena", "sensiblu") -> "Healthcare"
            containsAny(text, "benzinarie", "petrol", "rompetrol", "mol ", "omv", "lukoil", "motorina", "fuel") -> "Fuel"
            containsAny(text, "emag", "altex", "media galaxy", "electronic", "telefon", "laptop") -> "Electronics"
            containsAny(text, "h&m", "zara", "haine", "clothing", "fashion", "boutique", "new yorker") -> "Clothing"
            else -> "Uncategorized"
        }
    }

    private fun containsAny(text: String, vararg keywords: String) = keywords.any { text.contains(it) }

    private fun extractDate(lines: List<String>): Long? {
        val regex = Regex("""(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})""")
        for (line in lines) {
            val match = regex.find(line) ?: continue
            val (day, month, year) = match.destructured
            val fullYear = if (year.length == 2) 2000 + year.toInt() else year.toInt()
            return try {
                Calendar.getInstance().apply {
                    set(fullYear, month.toInt() - 1, day.toInt(), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } catch (_: Exception) { null }
        }
        return null
    }
}
