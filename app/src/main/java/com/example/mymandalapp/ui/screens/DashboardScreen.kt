package com.example.mymandalapp.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.core.pdf.ReceiptGenerator
import com.example.mymandalapp.core.pdf.ReportGenerator
import com.example.mymandalapp.ui.theme.ExpenseRed
import com.example.mymandalapp.ui.theme.IncomeGreen
import com.example.mymandalapp.ui.viewmodel.AuthViewModel
import com.example.mymandalapp.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddDonation: () -> Unit,
    onAddExpense: () -> Unit,
    onAddObjectDonation: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: FinanceViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    mandalId: String = "GUEST",
    year: String = "2026",
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(mandalId, year) {
        if (mandalId != "GUEST") {
            viewModel.loadData(mandalId, year)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ganpati_emblem),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp)
                        )
                        Column {
                            Text(state.mandalName.ifEmpty { "Mandal Finance" }, style = MaterialTheme.typography.titleMedium)
                            Text("Year $year", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Mandal Profile")
                    }
                    IconButton(
                        onClick = { 
                            scope.launch {
                                val uri = ReportGenerator.generateFullReport(
                                    context = context,
                                    profile = state.profile,
                                    year = year,
                                    openingBalance = state.openingBalance,
                                    transactions = state.transactions
                                )
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Report"))
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Report")
                    }
                    IconButton(
                        onClick = { 
                            authViewModel.logout()
                            onLogout()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error Loading Data", style = MaterialTheme.typography.headlineSmall, color = ExpenseRed)
                    Spacer(Modifier.height(8.dp))
                    Text(state.error ?: "Unknown Error", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadData(mandalId, year) }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BalanceSummaryCard(
                        opening = state.openingBalance,
                        income = state.totalIncome,
                        expense = state.totalExpense,
                        balance = state.currentBalance
                    )
                }

                item {
                    QuickActionsSection(
                        onAddDonation = onAddDonation,
                        onAddExpense = onAddExpense,
                        onAddObjectDonation = onAddObjectDonation
                    )
                }

                item {
                    PaymentBreakdownCard(
                        cashDonations = state.cashDonations,
                        upiDonations = state.upiDonations,
                        cashExpenses = state.cashExpenses,
                        upiExpenses = state.upiExpenses,
                        objectCount = state.objectDonationCount
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Recent Records", style = MaterialTheme.typography.titleLarge)
                }

                items(state.transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction, 
                        profile = state.profile,
                        onClick = { onEditTransaction(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onAddDonation: () -> Unit,
    onAddExpense: () -> Unit,
    onAddObjectDonation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quick Actions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                label = "DONATION",
                subLabel = "Add Money",
                icon = Icons.Default.Add,
                color = IncomeGreen,
                onClick = onAddDonation,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                label = "EXPENSE",
                subLabel = "Record Spend",
                icon = Icons.Default.Remove,
                color = ExpenseRed,
                onClick = onAddExpense,
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedCard(
            onClick = onAddObjectDonation,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("ADD OBJECT DONATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActionCard(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun BalanceSummaryCard(opening: Long, income: Long, expense: Long, balance: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CURRENT BALANCE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                Money.format(balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryItem("Opening", Money.format(opening), Modifier.weight(1f))
                SummaryItem("Income", Money.format(income), Modifier.weight(1f), color = IncomeGreen)
                SummaryItem("Expense", Money.format(expense), Modifier.weight(1f), color = ExpenseRed)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}

@Composable
fun PaymentBreakdownCard(
    cashDonations: Long,
    upiDonations: Long,
    cashExpenses: Long,
    upiExpenses: Long,
    objectCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Payment Breakdown", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "$objectCount Object Donations",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Donations", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, fontWeight = FontWeight.Bold)
                    BreakdownRow("Cash", Money.format(cashDonations))
                    BreakdownRow("UPI", Money.format(upiDonations))
                }
                VerticalDivider(modifier = Modifier.height(40.dp))
                Column(Modifier.weight(1f)) {
                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Bold)
                    BreakdownRow("Cash", Money.format(cashExpenses))
                    BreakdownRow("UPI", Money.format(upiExpenses))
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, profile: MandalProfile, onClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val label = when (transaction.type) {
                        TransactionType.INCOME -> transaction.donorName ?: "Donation"
                        TransactionType.EXPENSE -> transaction.description
                        TransactionType.OPENING_BALANCE -> "Opening Balance"
                        TransactionType.OBJECT_DONATION -> "OBJECT DONATION: ${transaction.itemName}"
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (transaction.type == TransactionType.INCOME && transaction.receiptNumber != null) {
                        ReceiptBadge(transaction.receiptNumber)
                    }
                    if (transaction.edited) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                "Edited",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                val subtext = if (transaction.type == TransactionType.OBJECT_DONATION) {
                    "Qty: ${transaction.quantity} ${transaction.unit} | Donor: ${transaction.donorName}"
                } else {
                    transaction.paymentMode.name
                }
                
                Text(
                    subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (transaction.type != TransactionType.OBJECT_DONATION) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val prefix = when (transaction.type) {
                        TransactionType.INCOME -> "+"
                        TransactionType.EXPENSE -> "-"
                        else -> ""
                    }
                    val color = when (transaction.type) {
                        TransactionType.INCOME -> IncomeGreen
                        TransactionType.EXPENSE -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        prefix + Money.format(transaction.amountPaise),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    if (transaction.type == TransactionType.INCOME) {
                        IconButton(onClick = {
                            scope.launch {
                                val uri = ReceiptGenerator.generateDonationReceipt(context, transaction, profile)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Receipt"))
                                }
                            }
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                Text(
                    "In-Kind",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReceiptBadge(number: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            number,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
