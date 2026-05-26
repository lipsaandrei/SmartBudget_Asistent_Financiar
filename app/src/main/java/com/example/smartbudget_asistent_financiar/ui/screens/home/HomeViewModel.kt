package com.example.smartbudget_asistent_financiar.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.data.local.entity.Budget
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.example.smartbudget_asistent_financiar.data.repository.BudgetRepository
import com.example.smartbudget_asistent_financiar.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CategorySpend(
    val category: String,
    val amount: Double,
    val fraction: Float,      // relative to top spender — used when no budget set
    val budget: Double? = null
)

data class HomeUiState(
    val thisMonthTotal: Double = 0.0,
    val lastMonthTotal: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val recentReceipts: List<Receipt> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    receiptRepository: ReceiptRepository,
    budgetRepository: BudgetRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        receiptRepository.getAllReceipts(),
        budgetRepository.getAllBudgets()
    ) { receipts, budgets -> buildUiState(receipts, budgets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildUiState(receipts: List<Receipt>, budgets: List<Budget>): HomeUiState {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)

        val thisMonthStart = monthStart(year, month)
        val thisMonthEnd = monthEnd(year, month)
        val lastMonthStart = monthStart(year, month - 1)
        val lastMonthEnd = monthEnd(year, month - 1)

        val thisMonth = receipts.filter { it.receiptDate in thisMonthStart..thisMonthEnd }
        val lastMonth = receipts.filter { it.receiptDate in lastMonthStart..lastMonthEnd }

        val budgetMap = budgets.associate { it.category to it.limitAmount }

        val entries = thisMonth
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.totalAmount } }
            .entries
            .sortedByDescending { it.value }

        val maxAmount = entries.firstOrNull()?.value?.takeIf { it > 0 } ?: 1.0
        val breakdown = entries.map {
            CategorySpend(
                category = it.key,
                amount = it.value,
                fraction = (it.value / maxAmount).toFloat(),
                budget = budgetMap[it.key]
            )
        }

        return HomeUiState(
            thisMonthTotal = thisMonth.sumOf { it.totalAmount },
            lastMonthTotal = lastMonth.sumOf { it.totalAmount },
            categoryBreakdown = breakdown,
            recentReceipts = receipts.take(5)
        )
    }

    private fun monthStart(year: Int, month: Int): Long = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun monthEnd(year: Int, month: Int): Long = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }.timeInMillis
}
