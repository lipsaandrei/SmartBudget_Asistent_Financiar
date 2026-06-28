package com.example.smartbudget_asistent_financiar.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartbudget_asistent_financiar.R
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
import com.example.smartbudget_asistent_financiar.ui.components.LanguagePicker
import com.example.smartbudget_asistent_financiar.ui.language.LanguageViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private val allCategories = listOf(
    "Groceries", "Food & Dining", "Healthcare", "Fuel", "Electronics", "Clothing", "Uncategorized"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onReceiptClick: (Long) -> Unit,
    onSignOut: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showBudgetSheet by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showBudgetSheet = true }) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = stringResource(R.string.budget_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { showAccountSheet = true }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item { SpendingAlertsCard(state.categoryBreakdown) }
            item { MonthSummaryCard(state.thisMonthTotal, state.lastMonthTotal) }

            if (state.categoryBreakdown.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_spending)) }
                item { CategoryBreakdownCard(state.categoryBreakdown) }
            }

            if (state.recentReceipts.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_recent_receipts)) }
                items(state.recentReceipts, key = { it.id }) { receipt ->
                    RecentReceiptRow(receipt = receipt, onClick = { onReceiptClick(receipt.id) })
                }
            }

            if (state.recentReceipts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.home_no_receipts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }

    if (showBudgetSheet) {
        val spendingMap = state.categoryBreakdown.associate { it.category to it.amount }
        BudgetSheet(
            spending = spendingMap,
            onDismiss = { showBudgetSheet = false }
        )
    }

    if (showAccountSheet) {
        AccountSheet(
            onDismiss = { showAccountSheet = false },
            onSignOut = {
                showAccountSheet = false
                onSignOut()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSheet(
    spending: Map<String, Double>,
    onDismiss: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val drafts = remember(budgets) {
        androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>().also { map ->
            allCategories.forEach { cat ->
                map[cat] = budgets[cat]?.let { "%.2f".format(it) } ?: ""
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.budget_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.budget_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            allCategories.forEach { category ->
                BudgetCategoryRow(
                    category = category,
                    spent = spending[category] ?: 0.0,
                    draft = drafts[category] ?: "",
                    onDraftChange = { drafts[category] = it }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    allCategories.forEach { cat ->
                        val amount = drafts[cat]?.replace(",", ".")?.toDoubleOrNull()
                        if (amount != null && amount > 0) viewModel.setBudget(cat, amount)
                        else viewModel.removeBudget(cat)
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_apply)) }
        }
    }
}

@Composable
private fun BudgetCategoryRow(
    category: String,
    spent: Double,
    draft: String,
    onDraftChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (spent > 0) {
                Text(
                    stringResource(R.string.label_spent, spent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text(stringResource(R.string.label_no_limit)) },
            suffix = { Text("RON") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
}

private data class BudgetAlert(val category: String, val ratio: Double, val exceeded: Boolean)

@Composable
private fun SpendingAlertsCard(breakdown: List<CategorySpend>) {
    val alerts = breakdown.mapNotNull { item ->
        val budget = item.budget?.takeIf { it > 0 } ?: return@mapNotNull null
        val ratio = item.amount / budget
        when {
            ratio >= 1.0 -> BudgetAlert(item.category, ratio, exceeded = true)
            ratio >= 0.75 -> BudgetAlert(item.category, ratio, exceeded = false)
            else -> null
        }
    }
    if (alerts.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.budget_alerts_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            alerts.forEach { budgetAlert ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (budgetAlert.exceeded) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (budgetAlert.exceeded) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (budgetAlert.exceeded)
                            stringResource(R.string.alert_over_budget, budgetAlert.category, (budgetAlert.ratio - 1) * 100)
                        else
                            stringResource(R.string.alert_budget_used, budgetAlert.category, budgetAlert.ratio * 100),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (budgetAlert.exceeded) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(thisMonth: Double, lastMonth: Double) {
    val diff = thisMonth - lastMonth
    val pct = if (lastMonth > 0) (diff / lastMonth * 100).toInt() else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_this_month),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.2f RON".format(thisMonth),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (pct != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val sign = if (diff >= 0) "+" else "-"
                val color = if (diff <= 0) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                Text(
                    text = stringResource(R.string.home_vs_last_month, sign, abs(pct), abs(diff)),
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            } else if (lastMonth == 0.0 && thisMonth == 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_start_scanning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: List<CategorySpend>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            breakdown.forEach { item -> CategoryRow(item) }
        }
    }
}

@Composable
private fun CategoryRow(item: CategorySpend) {
    val budget = item.budget

    val progress: Float
    val indicatorColor: Color
    val budgetLabel: String?

    if (budget != null && budget > 0) {
        val ratio = item.amount / budget
        progress = min(ratio.toFloat(), 1f)
        indicatorColor = when {
            ratio >= 1.0 -> MaterialTheme.colorScheme.error
            ratio >= 0.75 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
        budgetLabel = "/ %.0f RON".format(budget)
    } else {
        progress = item.fraction
        indicatorColor = MaterialTheme.colorScheme.primary
        budgetLabel = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.category, style = MaterialTheme.typography.bodyMedium)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.2f RON".format(item.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (budgetLabel != null) {
                    Text(
                        budgetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            strokeCap = StrokeCap.Round,
            color = indicatorColor
        )
    }
}

@Composable
private fun RecentReceiptRow(receipt: Receipt, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(receipt.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    receipt.receiptDate.toFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "%.2f RON".format(receipt.totalAmount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
    languageViewModel: LanguageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.isGuest) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = viewModel.initials,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = viewModel.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = viewModel.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LanguagePicker(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = {
                    languageViewModel.setLanguage(it)
                    (context as? android.app.Activity)?.recreate()
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (viewModel.isGuest) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_sign_in_google))
                }
            } else {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.action_sign_out))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun Long.toFormattedDate(): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
