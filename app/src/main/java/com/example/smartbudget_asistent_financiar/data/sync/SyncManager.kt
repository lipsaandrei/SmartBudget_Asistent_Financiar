package com.example.smartbudget_asistent_financiar.data.sync

import com.example.smartbudget_asistent_financiar.data.local.dao.BudgetDao
import com.example.smartbudget_asistent_financiar.data.local.dao.ReceiptDao
import com.example.smartbudget_asistent_financiar.data.remote.FirestoreSource
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val receiptDao: ReceiptDao,
    private val budgetDao: BudgetDao
) {
    suspend fun syncOnSignIn() {
        withTimeout(10_000L) {
            syncReceipts()
            syncBudgets()
        }
    }

    private suspend fun syncReceipts() {
        val local = receiptDao.getAllReceiptsOnce()
        val remote = firestoreSource.fetchAllReceipts()
        when {
            remote.isEmpty() && local.isNotEmpty() -> local.forEach { firestoreSource.uploadReceipt(it) }
            remote.isNotEmpty() -> remote.forEach { receiptDao.insert(it) }
        }
    }

    private suspend fun syncBudgets() {
        val local = budgetDao.getAllBudgetsOnce()
        val remote = firestoreSource.fetchAllBudgets()
        when {
            remote.isEmpty() && local.isNotEmpty() -> local.forEach { firestoreSource.uploadBudget(it) }
            remote.isNotEmpty() -> remote.forEach { budgetDao.upsert(it) }
        }
    }

    suspend fun clearLocalData() {
        receiptDao.clearAll()
        budgetDao.clearAll()
    }
}
