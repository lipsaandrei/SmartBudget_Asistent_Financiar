package com.example.smartbudget_asistent_financiar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartbudget_asistent_financiar.data.local.dao.BudgetDao
import com.example.smartbudget_asistent_financiar.data.local.dao.ReceiptDao
import com.example.smartbudget_asistent_financiar.data.local.entity.Budget
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt

@Database(
    entities = [Receipt::class, Budget::class],
    version = 2,
    exportSchema = false
)
abstract class SmartBudgetDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` " +
                    "(`category` TEXT NOT NULL, `limitAmount` REAL NOT NULL, " +
                    "`currency` TEXT NOT NULL DEFAULT 'RON', PRIMARY KEY(`category`))"
                )
            }
        }
    }
}
