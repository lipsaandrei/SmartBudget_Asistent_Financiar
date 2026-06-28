package com.example.smartbudget_asistent_financiar.ui.language

import androidx.lifecycle.ViewModel
import com.example.smartbudget_asistent_financiar.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

val SUPPORTED_LANGUAGES = listOf("English", "Romanian")

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(languageManager.getLanguage())
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setLanguage(language: String) {
        languageManager.setLanguage(language)
        _selectedLanguage.value = language
    }
}
