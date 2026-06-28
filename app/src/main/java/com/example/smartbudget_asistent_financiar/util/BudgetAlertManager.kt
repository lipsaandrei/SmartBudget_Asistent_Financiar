package com.example.smartbudget_asistent_financiar.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.smartbudget_asistent_financiar.R
import com.example.smartbudget_asistent_financiar.data.repository.BudgetRepository
import com.example.smartbudget_asistent_financiar.data.repository.ReceiptRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

enum class AlertLevel { None, Warning, Exceeded }

@Singleton
class BudgetAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receiptRepository: ReceiptRepository,
    private val budgetRepository: BudgetRepository
) {
    companion object {
        const val CHANNEL_ID = "budget_alerts"
    }

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Notifies when spending approaches or exceeds a category budget" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun checkAndNotify(category: String): AlertLevel {
        val budget = budgetRepository.getByCategory(category) ?: return AlertLevel.None
        if (budget.limitAmount <= 0) return AlertLevel.None

        val (monthStart, monthEnd) = currentMonthRange()
        val spent = receiptRepository.getCategoryTotalInRange(category, monthStart, monthEnd)
        val ratio = spent / budget.limitAmount

        return when {
            ratio >= 1.0 -> {
                postNotification(category, spent, budget.limitAmount, AlertLevel.Exceeded)
                AlertLevel.Exceeded
            }
            ratio >= 0.75 -> {
                postNotification(category, spent, budget.limitAmount, AlertLevel.Warning)
                AlertLevel.Warning
            }
            else -> AlertLevel.None
        }
    }

    private fun postNotification(category: String, spent: Double, limit: Double, level: AlertLevel) {
        val (title, body) = if (level == AlertLevel.Exceeded) {
            "Budget exceeded – $category" to
                "You've spent %.2f RON (%.0f%% over your %.2f RON limit).".format(
                    spent, (spent / limit - 1) * 100, limit
                )
        } else {
            "Budget warning – $category" to
                "You've used %.0f%% of your %.2f RON budget (%.2f RON spent).".format(
                    (spent / limit) * 100, limit, spent
                )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(category.hashCode(), notification)
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val start = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        return start to end
    }
}
