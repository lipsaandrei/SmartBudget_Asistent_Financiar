package com.example.smartbudget_asistent_financiar.data.remote

import com.example.smartbudget_asistent_financiar.data.local.entity.Budget
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─── DTOs (no Room annotations → safe for Firestore deserialization) ─────────

data class ReceiptDto(
    val id: Long = 0,
    val merchant: String = "",
    val totalAmount: Double = 0.0,
    val currency: String = "RON",
    val category: String = "Uncategorized",
    val receiptDate: Long = 0L,
    val scannedAt: Long = 0L,
    val rawOcrText: String = ""
) {
    fun toEntity() = Receipt(id, merchant, totalAmount, currency, category, receiptDate, scannedAt, rawOcrText)
}

data class BudgetDto(
    val category: String = "",
    val limitAmount: Double = 0.0,
    val currency: String = "RON"
) {
    fun toEntity() = Budget(category, limitAmount, currency)
}

fun Receipt.toDto() = ReceiptDto(id, merchant, totalAmount, currency, category, receiptDate, scannedAt, rawOcrText)
fun Budget.toDto() = BudgetDto(category, limitAmount, currency)

// ─── Source ───────────────────────────────────────────────────────────────────

@Singleton
class FirestoreSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val uid get() = auth.currentUser?.uid

    private val receiptsCol get() = uid?.let {
        firestore.collection("users").document(it).collection("receipts")
    }

    private val budgetsCol get() = uid?.let {
        firestore.collection("users").document(it).collection("budgets")
    }

    // ── Receipts ──────────────────────────────────────────────────────────────

    suspend fun uploadReceipt(receipt: Receipt) {
        receiptsCol?.document(receipt.id.toString())?.set(receipt.toDto())?.await()
    }

    suspend fun deleteReceipt(id: Long) {
        receiptsCol?.document(id.toString())?.delete()?.await()
    }

    suspend fun fetchAllReceipts(): List<Receipt> =
        receiptsCol?.get()?.await()
            ?.documents
            ?.mapNotNull { it.toObject(ReceiptDto::class.java)?.toEntity() }
            ?: emptyList()

    // ── Budgets ───────────────────────────────────────────────────────────────

    suspend fun uploadBudget(budget: Budget) {
        budgetsCol?.document(budget.category)?.set(budget.toDto())?.await()
    }

    suspend fun deleteBudget(category: String) {
        budgetsCol?.document(category)?.delete()?.await()
    }

    suspend fun fetchAllBudgets(): List<Budget> =
        budgetsCol?.get()?.await()
            ?.documents
            ?.mapNotNull { it.toObject(BudgetDto::class.java)?.toEntity() }
            ?: emptyList()
}
