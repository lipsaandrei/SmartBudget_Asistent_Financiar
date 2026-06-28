package com.example.smartbudget_asistent_financiar.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("smartbudget_prefs", Context.MODE_PRIVATE)

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT) ?: DEFAULT

    fun setLanguage(language: String) = prefs.edit().putString(KEY_LANGUAGE, language).apply()

    companion object {
        const val DEFAULT = "English"
        private const val KEY_LANGUAGE = "language"
    }
}
