package com.example.smartbudget_asistent_financiar.data.repository

import com.example.smartbudget_asistent_financiar.data.local.dao.BudgetDao
import com.example.smartbudget_asistent_financiar.data.local.entity.Budget
import com.example.smartbudget_asistent_financiar.data.remote.FirestoreSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    private val firestore: FirestoreSource
) {
    fun getAllBudgets(): Flow<List<Budget>> = dao.getAllBudgets()

    suspend fun upsert(budget: Budget) {
        dao.upsert(budget)
        runCatching { firestore.uploadBudget(budget) }
    }

    suspend fun deleteByCategory(category: String) {
        dao.deleteByCategory(category)
        runCatching { firestore.deleteBudget(category) }
    }
}
