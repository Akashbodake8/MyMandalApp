package com.example.mymandalapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.finance.PaymentMode
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import com.example.mymandalapp.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.example.mymandalapp.R

import com.example.mymandalapp.core.utils.ValidationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = viewModel(),
    mandalId: String = "GUEST",
    year: String = "2026"
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Others") }
    var vendor by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    
    // Error States
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf("Decoration", "Prasad", "Music", "Electricity", "Others")
    var expanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                        Text("Add Expense")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = null },
                label = { Text("Expense Title *") },
                isError = titleError != null,
                supportingText = { if (titleError != null) Text(titleError!!) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; amountError = null },
                label = { Text("Amount *") },
                isError = amountError != null,
                supportingText = { if (amountError != null) Text(amountError!!) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                prefix = { Text("₹") }
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (!state.isSaving) expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    enabled = !state.isSaving
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = vendor,
                onValueChange = { vendor = it },
                label = { Text("Vendor Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            Text("Payment Mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PaymentModeOption("Cash", paymentMode == PaymentMode.CASH) { 
                    if (!state.isSaving) paymentMode = PaymentMode.CASH 
                }
                PaymentModeOption("UPI", paymentMode == PaymentMode.UPI) { 
                    if (!state.isSaving) paymentMode = PaymentMode.UPI 
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    var hasError = false
                    if (!ValidationUtils.isNotEmpty(title)) { titleError = "Please enter expense title."; hasError = true }
                    if (!ValidationUtils.isValidAmount(amount)) { amountError = "Amount must be greater than ₹0."; hasError = true }
                    
                    if (!hasError) {
                        val amountPaise = Money.fromRupees(amount)
                        if (amountPaise > state.currentBalance) {
                            scope.launch { snackbarHostState.showSnackbar("Warning: Insufficient Balance. Spending beyond available funds.") }
                        }

                        scope.launch {
                            try {
                                val transaction = Transaction(
                                    type = TransactionType.EXPENSE,
                                    amountPaise = amountPaise,
                                    description = ValidationUtils.trim(title),
                                    expenseCategory = category,
                                    vendorName = ValidationUtils.trim(vendor),
                                    paymentMode = paymentMode
                                )
                                viewModel.saveTransaction(mandalId, year, transaction)
                                onBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Expense")
                }
            }
        }
    }
}
