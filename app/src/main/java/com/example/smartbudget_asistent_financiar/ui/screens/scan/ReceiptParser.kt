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

    // A parsed date is trusted only if it is plausible for a receipt: not in the
    // future and at most one year old. OCR noise (CIF, phone numbers, bon fiscal
    // ids) can match the pattern, so implausible candidates are skipped and the
    // caller falls back to the scan time.
    private fun extractDate(lines: List<String>): Long? {
        val regex = Regex("""(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})""")
        val now = System.currentTimeMillis()
        val oneYearAgo = now - 366L * 24 * 60 * 60 * 1000
        val endOfToday = now + 24L * 60 * 60 * 1000
        for (line in lines) {
            for (match in regex.findAll(line)) {
                val (dayStr, monthStr, yearStr) = match.destructured
                val day = dayStr.toInt()
                val month = monthStr.toInt()
                if (day !in 1..31 || month !in 1..12) continue
                val fullYear = if (yearStr.length == 2) 2000 + yearStr.toInt() else yearStr.toInt()
                val millis = try {
                    Calendar.getInstance().apply {
                        isLenient = false
                        clear()
                        set(fullYear, month - 1, day)
                    }.timeInMillis
                } catch (_: Exception) { continue }
                if (millis in oneYearAgo..endOfToday) return millis
            }
        }
        return null
    }
}
