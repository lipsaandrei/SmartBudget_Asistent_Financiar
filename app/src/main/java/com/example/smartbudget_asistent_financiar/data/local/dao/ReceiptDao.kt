package com.example.smartbudget_asistent_financiar.data.local.dao

import androidx.room.*
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: Receipt): Long

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    @Query("SELECT * FROM receipts ORDER BY receiptDate DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): Receipt?

    @Query("SELECT * FROM receipts WHERE receiptDate BETWEEN :from AND :to ORDER BY receiptDate DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE category = :category ORDER BY receiptDate DESC")
    fun getByCategory(category: String): Flow<List<Receipt>>

    @Query("SELECT SUM(totalAmount) FROM receipts WHERE receiptDate BETWEEN :from AND :to")
    suspend fun getTotalInRange(from: Long, to: Long): Double?
}
