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

    suspend fun clearLocalData() {
        receiptDao.deleteAll()
        budgetDao.deleteAll()
    }

    // Room is cleared on sign-out, so anything local at sign-in time is guest data.
    // Guest data migrates to the account only on its first sign-in (empty cloud);
    // an account that already has cloud data replaces whatever the guest scanned.
    private suspend fun syncReceipts() {
        val guestReceipts = receiptDao.getAllReceiptsOnce()
        val remote = firestoreSource.fetchAllReceipts()
        if (remote.isEmpty()) {
            guestReceipts.forEach { firestoreSource.uploadReceipt(it) }
        } else {
            receiptDao.deleteAll()
            remote.forEach { receiptDao.insert(it) }
        }
    }

    private suspend fun syncBudgets() {
        val guestBudgets = budgetDao.getAllBudgetsOnce()
        val remote = firestoreSource.fetchAllBudgets()
        if (remote.isEmpty()) {
            guestBudgets.forEach { firestoreSource.uploadBudget(it) }
        } else {
            budgetDao.deleteAll()
            remote.forEach { budgetDao.upsert(it) }
        }
    }
}
