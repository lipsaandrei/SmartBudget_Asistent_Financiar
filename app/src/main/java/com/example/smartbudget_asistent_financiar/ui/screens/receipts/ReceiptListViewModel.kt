package com.example.smartbudget_asistent_financiar.ui.screens.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.example.smartbudget_asistent_financiar.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    val receipts: StateFlow<List<Receipt>> = repository.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(receipt: Receipt) {
        viewModelScope.launch { repository.delete(receipt) }
    }
}
