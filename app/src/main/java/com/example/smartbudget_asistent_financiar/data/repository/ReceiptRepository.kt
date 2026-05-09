package com.example.smartbudget_asistent_financiar.data.repository

import com.example.smartbudget_asistent_financiar.data.local.dao.ReceiptDao
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(private val dao: ReceiptDao) {

    fun getAllReceipts(): Flow<List<Receipt>> = dao.getAllReceipts()

    fun getByDateRange(from: Long, to: Long): Flow<List<Receipt>> = dao.getByDateRange(from, to)

    fun getByCategory(category: String): Flow<List<Receipt>> = dao.getByCategory(category)

    suspend fun getById(id: Long): Receipt? = dao.getById(id)

    suspend fun insert(receipt: Receipt): Long = dao.insert(receipt)

    suspend fun update(receipt: Receipt) = dao.update(receipt)

    suspend fun delete(receipt: Receipt) = dao.delete(receipt)

    suspend fun getTotalInRange(from: Long, to: Long): Double = dao.getTotalInRange(from, to) ?: 0.0
}
