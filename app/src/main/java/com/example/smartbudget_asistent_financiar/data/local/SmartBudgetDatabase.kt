package com.example.smartbudget_asistent_financiar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartbudget_asistent_financiar.data.local.dao.ReceiptDao
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt

@Database(
    entities = [Receipt::class],
    version = 1,
    exportSchema = false
)
abstract class SmartBudgetDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
}
