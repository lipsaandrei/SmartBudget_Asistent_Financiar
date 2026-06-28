package com.example.smartbudget_asistent_financiar.data.repository

import com.example.smartbudget_asistent_financiar.data.local.dao.ReceiptDao
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.example.smartbudget_asistent_financiar.data.remote.FirestoreSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val dao: ReceiptDao,
    private val firestore: FirestoreSource
) {
    fun getAllReceipts(): Flow<List<Receipt>> = dao.getAllReceipts()

    fun getByDateRange(from: Long, to: Long): Flow<List<Receipt>> = dao.getByDateRange(from, to)

    fun getByCategory(category: String): Flow<List<Receipt>> = dao.getByCategory(category)

    suspend fun getById(id: Long): Receipt? = dao.getById(id)

    suspend fun insert(receipt: Receipt): Long {
        val id = dao.insert(receipt)
        runCatching { firestore.uploadReceipt(receipt.copy(id = id)) }
        return id
    }

    suspend fun update(receipt: Receipt) {
        dao.update(receipt)
        runCatching { firestore.uploadReceipt(receipt) }
    }

    suspend fun delete(receipt: Receipt) {
        dao.delete(receipt)
        runCatching { firestore.deleteReceipt(receipt.id) }
    }

    suspend fun getTotalInRange(from: Long, to: Long): Double = dao.getTotalInRange(from, to) ?: 0.0

    suspend fun getCategoryTotalInRange(category: String, from: Long, to: Long): Double =
        dao.getCategoryTotalInRange(category, from, to) ?: 0.0

    suspend fun getAllReceiptsOnce(): List<com.example.smartbudget_asistent_financiar.data.local.entity.Receipt> =
        dao.getAllReceiptsOnce()
}
