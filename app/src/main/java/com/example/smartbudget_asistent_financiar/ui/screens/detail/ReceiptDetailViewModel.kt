package com.example.smartbudget_asistent_financiar.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.example.smartbudget_asistent_financiar.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptDraft(
    val merchant: String,
    val amount: String,
    val category: String
)

sealed class DetailUiState {
    object Loading : DetailUiState()
    object NotFound : DetailUiState()
    data class Found(val receipt: Receipt) : DetailUiState()
    data class Editing(val receipt: Receipt, val draft: ReceiptDraft) : DetailUiState()
}

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReceiptRepository
) : ViewModel() {

    private val receiptId: Long = checkNotNull(savedStateHandle["receiptId"])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val receipt = repository.getById(receiptId)
            _uiState.value = if (receipt != null) DetailUiState.Found(receipt) else DetailUiState.NotFound
        }
    }

    fun startEdit() {
        val found = _uiState.value as? DetailUiState.Found ?: return
        _uiState.value = DetailUiState.Editing(
            receipt = found.receipt,
            draft = ReceiptDraft(
                merchant = found.receipt.merchant,
                amount = "%.2f".format(found.receipt.totalAmount),
                category = found.receipt.category
            )
        )
    }

    fun cancelEdit() {
        val editing = _uiState.value as? DetailUiState.Editing ?: return
        _uiState.value = DetailUiState.Found(editing.receipt)
    }

    fun updateDraftMerchant(value: String) {
        val editing = _uiState.value as? DetailUiState.Editing ?: return
        _uiState.value = editing.copy(draft = editing.draft.copy(merchant = value))
    }

    fun updateDraftAmount(value: String) {
        val editing = _uiState.value as? DetailUiState.Editing ?: return
        _uiState.value = editing.copy(draft = editing.draft.copy(amount = value))
    }

    fun updateDraftCategory(value: String) {
        val editing = _uiState.value as? DetailUiState.Editing ?: return
        _uiState.value = editing.copy(draft = editing.draft.copy(category = value))
    }

    fun saveEdit() {
        val editing = _uiState.value as? DetailUiState.Editing ?: return
        val amount = editing.draft.amount.replace(",", ".").toDoubleOrNull()
            ?: editing.receipt.totalAmount
        val updated = editing.receipt.copy(
            merchant = editing.draft.merchant.ifBlank { editing.receipt.merchant },
            totalAmount = amount,
            category = editing.draft.category
        )
        viewModelScope.launch {
            repository.update(updated)
            _uiState.value = DetailUiState.Found(updated)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val receipt = when (val s = _uiState.value) {
            is DetailUiState.Found -> s.receipt
            is DetailUiState.Editing -> s.receipt
            else -> return
        }
        viewModelScope.launch {
            repository.delete(receipt)
            onDeleted()
        }
    }
}
