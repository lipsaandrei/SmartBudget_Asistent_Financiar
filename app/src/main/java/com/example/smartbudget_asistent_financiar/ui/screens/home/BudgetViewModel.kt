package com.example.smartbudget_asistent_financiar.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.data.local.entity.Budget
import com.example.smartbudget_asistent_financiar.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    val budgets: StateFlow<Map<String, Double>> = repository.getAllBudgets()
        .map { list -> list.associate { it.category to it.limitAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setBudget(category: String, limitAmount: Double) {
        viewModelScope.launch { repository.upsert(Budget(category, limitAmount)) }
    }

    fun removeBudget(category: String) {
        viewModelScope.launch { repository.deleteByCategory(category) }
    }
}
