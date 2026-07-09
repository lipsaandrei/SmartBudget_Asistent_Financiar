package com.example.smartbudget_asistent_financiar.ui.screens.scan

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.example.smartbudget_asistent_financiar.data.repository.ReceiptRepository
import com.example.smartbudget_asistent_financiar.util.BudgetAlertManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Processing : ScanUiState()
    data class Review(
        val merchant: String,
        val amount: String,
        val category: String,
        val date: Long,
        val rawText: String
    ) : ScanUiState()
    object Saving : ScanUiState()
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val alertManager: BudgetAlertManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy) {
        _uiState.value = ScanUiState.Processing
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            _uiState.value = ScanUiState.Idle
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(inputImage)
            .addOnSuccessListener { result ->
                imageProxy.close()
                val parsed = ReceiptParser.parse(result.text)
                _uiState.value = ScanUiState.Review(
                    merchant = parsed.merchant,
                    amount = "%.2f".format(parsed.amount),
                    category = parsed.category,
                    date = parsed.date,
                    rawText = result.text
                )
            }
            .addOnFailureListener {
                imageProxy.close()
                _uiState.value = ScanUiState.Idle
            }
    }

    fun updateMerchant(value: String) {
        val s = _uiState.value as? ScanUiState.Review ?: return
        _uiState.value = s.copy(merchant = value)
    }

    fun updateAmount(value: String) {
        val s = _uiState.value as? ScanUiState.Review ?: return
        _uiState.value = s.copy(amount = value)
    }

    fun updateCategory(value: String) {
        val s = _uiState.value as? ScanUiState.Review ?: return
        _uiState.value = s.copy(category = value)
    }

    fun updateDate(value: Long) {
        val s = _uiState.value as? ScanUiState.Review ?: return
        _uiState.value = s.copy(date = value)
    }

    fun save(onSaved: (Long) -> Unit) {
        val s = _uiState.value as? ScanUiState.Review ?: return
        val amount = s.amount.replace(",", ".").toDoubleOrNull() ?: 0.0
        _uiState.value = ScanUiState.Saving
        viewModelScope.launch {
            val id = repository.insert(
                Receipt(
                    merchant = s.merchant.ifBlank { "Unknown" },
                    totalAmount = amount,
                    category = s.category,
                    receiptDate = s.date,
                    scannedAt = System.currentTimeMillis(),
                    rawOcrText = s.rawText
                )
            )
            alertManager.checkAndNotify(s.category)
            onSaved(id)
        }
    }

    fun processUri(context: Context, uri: Uri) {
        _uiState.value = ScanUiState.Processing
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(inputImage)
                .addOnSuccessListener { result ->
                    val parsed = ReceiptParser.parse(result.text)
                    _uiState.value = ScanUiState.Review(
                        merchant = parsed.merchant,
                        amount = "%.2f".format(parsed.amount),
                        category = parsed.category,
                        date = parsed.date,
                        rawText = result.text
                    )
                }
                .addOnFailureListener {
                    _uiState.value = ScanUiState.Idle
                }
        } catch (_: IOException) {
            _uiState.value = ScanUiState.Idle
        }
    }

    fun reset() { _uiState.value = ScanUiState.Idle }
}
