package com.example.smartbudget_asistent_financiar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val totalAmount: Double,
    val currency: String = "RON",
    val category: String = "Uncategorized",
    val receiptDate: Long,
    val scannedAt: Long,
    val rawOcrText: String = ""
)
