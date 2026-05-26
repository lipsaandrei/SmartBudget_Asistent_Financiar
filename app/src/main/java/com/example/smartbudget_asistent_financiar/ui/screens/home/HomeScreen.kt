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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartbudget_asistent_financiar.data.local.entity.Receipt
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
                title = { Text("SmartBudget") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showBudgetSheet = true }) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = "Set budgets",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { showAccountSheet = true }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Account",
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
            item { MonthSummaryCard(state.thisMonthTotal, state.lastMonthTotal) }

            if (state.categoryBreakdown.isNotEmpty()) {
                item { SectionLabel("Spending by Category") }
                item { CategoryBreakdownCard(state.categoryBreakdown) }
            }

            if (state.recentReceipts.isNotEmpty()) {
                item { SectionLabel("Recent Receipts") }
                items(state.recentReceipts, key = { it.id }) { receipt ->
                    RecentReceiptRow(receipt = receipt, onClick = { onReceiptClick(receipt.id) })
                }
            }

            if (state.recentReceipts.isEmpty()) {
                item {
                    Text(
                        text = "No receipts yet — tap Scan to add your first one.",
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

    // Local draft: category → amount string; initialised from persisted budgets
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
            Text("Monthly Budget Limits", style = MaterialTheme.typography.titleLarge)
            Text(
                "Leave empty to remove a limit.",
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
            ) { Text("Apply") }
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
                    "Spent: %.2f RON".format(spent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text("No limit") },
            suffix = { Text("RON") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
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
                text = "This Month",
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
                    text = "$sign${abs(pct)}% vs last month (%.2f RON)".format(abs(diff)),
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            } else if (lastMonth == 0.0 && thisMonth == 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Start scanning to track your spending",
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
    viewModel: AccountViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circle with initials
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

            if (viewModel.isGuest) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
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
                    Text("Sign out")
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
