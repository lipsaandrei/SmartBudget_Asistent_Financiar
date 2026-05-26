package com.example.smartbudget_asistent_financiar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartbudget_asistent_financiar.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsOnce(): List<Budget>

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
