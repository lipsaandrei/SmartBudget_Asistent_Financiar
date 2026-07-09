package com.example.smartbudget_asistent_financiar.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.BuildConfig
import com.example.smartbudget_asistent_financiar.data.repository.BudgetRepository
import com.example.smartbudget_asistent_financiar.data.repository.ReceiptRepository
import com.example.smartbudget_asistent_financiar.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val budgetRepository: BudgetRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiHistory = mutableListOf<Pair<String, String>>()
    private var cachedFinancialContext: String? = null

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        _messages.value = _messages.value + ChatMessage(userText, isUser = true)
        _isLoading.value = true

        viewModelScope.launch {
            val placeholderIndex = _messages.value.size
            _messages.value = _messages.value + ChatMessage("", isUser = false, isLoading = true)

            try {
                val financialContext = cachedFinancialContext ?: buildFinancialContext().also { cachedFinancialContext = it }
                val prompt = buildSystemPrompt(financialContext)
                val response = callGeminiApi(userText, prompt)

                apiHistory.add("user" to userText)
                apiHistory.add("model" to response)

                val updated = _messages.value.toMutableList()
                updated[placeholderIndex] = ChatMessage(response, isUser = false)
                _messages.value = updated
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("quota", ignoreCase = true) == true ||
                    e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                        "API quota exceeded. Please check your usage limits."
                    e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                    e.message?.contains("403", ignoreCase = true) == true ->
                        "Invalid API key. Check local.properties — key should start with AIza."
                    else -> "Something went wrong: ${e.message ?: "unknown error"}. Please try again."
                }
                val updated = _messages.value.toMutableList()
                updated[placeholderIndex] = ChatMessage(msg, isUser = false)
                _messages.value = updated
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun callGeminiApi(userText: String, sysPrompt: String): String = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        apiHistory.forEach { (role, text) ->
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userText)))
        })

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", sysPrompt)))
            })
            put("contents", contents)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseStr = response.body?.string() ?: throw Exception("Empty response from server")

        if (!response.isSuccessful) {
            val errorMsg = runCatching {
                JSONObject(responseStr).getJSONObject("error").getString("message")
            }.getOrElse { "HTTP ${response.code}: $responseStr" }
            throw Exception(errorMsg)
        }

        val parts = JSONObject(responseStr)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")

        buildString {
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (!part.optBoolean("thought", false)) {
                    append(part.optString("text", ""))
                }
            }
        }.ifBlank { "I couldn't generate a response. Please try again." }
    }

    private suspend fun buildFinancialContext(): String {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val monthStart = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis

        val allReceipts = receiptRepository.getAllReceiptsOnce()
        val thisMonthReceipts = allReceipts.filter { it.receiptDate in monthStart..monthEnd }
        val budgets = budgetRepository.getAllBudgetsOnce()
        val budgetMap = budgets.associate { it.category to it.limitAmount }

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        val totalSpent = thisMonthReceipts.sumOf { it.totalAmount }

        val categoryBreakdown = thisMonthReceipts
            .groupBy { it.category }
            .mapValues { (_, receipts) -> receipts.sumOf { it.totalAmount } }
            .entries
            .sortedByDescending { it.value }

        return buildString {
            appendLine("Current month: $monthName")
            appendLine("Total spent this month: %.2f RON".format(totalSpent))
            appendLine()
            appendLine("Spending by category:")
            categoryBreakdown.forEach { (cat, amount) ->
                val budget = budgetMap[cat]
                if (budget != null) {
                    val pct = (amount / budget * 100).toInt()
                    appendLine("  - $cat: %.2f / %.2f RON budget ($pct%%)".format(amount, budget))
                } else {
                    appendLine("  - $cat: %.2f RON (no budget set)".format(amount))
                }
            }
            appendLine()
            appendLine("Recent receipts (latest first):")
            allReceipts.take(8).forEach { receipt ->
                val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(receipt.receiptDate))
                appendLine("  - $date: ${receipt.merchant} [${receipt.category}] ${"%.2f".format(receipt.totalAmount)} RON")
            }
            if (budgets.isNotEmpty()) {
                appendLine()
                appendLine("Monthly budget limits:")
                budgets.forEach { budget ->
                    appendLine("  - ${budget.category}: %.2f RON".format(budget.limitAmount))
                }
            }
        }
    }

    private fun buildSystemPrompt(financialContext: String): String {
        val language = languageManager.getLanguage()
        return """
            You are a smart financial assistant built into SmartBudget, a personal finance app.
            Always respond in $language, regardless of what language the user writes in.
            You have full access to the user's real spending data shown below. Use it to answer their questions accurately.
            Be concise, friendly, and actionable. Format money as RON (Romanian Lei).
            If you notice concerning spending patterns or budget risks, mention them proactively.
            Do not make up data — only reference what is in the financial context.

            --- USER FINANCIAL DATA ---
            $financialContext
            --- END OF DATA ---
        """.trimIndent()
    }
}
