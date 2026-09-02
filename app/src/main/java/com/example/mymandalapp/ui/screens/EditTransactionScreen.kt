package com.example.mymandalapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.finance.PaymentMode
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import com.example.mymandalapp.core.utils.ValidationUtils
import com.example.mymandalapp.ui.viewmodel.FinanceViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    txId: String,
    onBack: () -> Unit,
    viewModel: FinanceViewModel = viewModel(),
    mandalId: String = "GUEST",
    year: String = "2026"
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var existingTx by remember { mutableStateOf<Transaction?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Form fields
    var donorName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    
    // Object Specific
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Kg") }

    LaunchedEffect(txId) {
        // Ensure data is loaded
        if (mandalId != "GUEST") {
            viewModel.loadData(mandalId, year)
        }
        
        // Wait for transactions to be available in state
        snapshotFlow { state.transactions }
            .filter { it.isNotEmpty() }
            .first()
            .let { transactions ->
                val tx = transactions.find { it.id == txId }
                if (tx != null) {
                    existingTx = tx
                    donorName = tx.donorName ?: ""
                    amount = if (tx.type != TransactionType.OBJECT_DONATION) Money.toRupees(tx.amountPaise).toString() else ""
                    description = tx.description
                    paymentMode = tx.paymentMode
                    itemName = tx.itemName ?: ""
                    quantity = tx.quantity ?: ""
                    unit = tx.unit ?: "Kg"
                    isLoading = false
                } else {
                    onBack()
                }
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
                        Text("Edit Record")
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
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Editing: ${existingTx?.type?.name}", 
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (existingTx?.type == TransactionType.INCOME || existingTx?.type == TransactionType.OBJECT_DONATION) {
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { donorName = it },
                        label = { Text("Donor Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    )
                }

                if (existingTx?.type == TransactionType.OBJECT_DONATION) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    )
                } else {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        prefix = { Text("₹") }
                    )
                    
                    if (existingTx?.type == TransactionType.EXPENSE) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Expense Title *") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving
                        )
                    }

                    Text("Payment Mode", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PaymentModeOption("Cash", paymentMode == PaymentMode.CASH) { if (!state.isSaving) paymentMode = PaymentMode.CASH }
                        PaymentModeOption("UPI", paymentMode == PaymentMode.UPI) { if (!state.isSaving) paymentMode = PaymentMode.UPI }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        existingTx?.let { tx ->
                            scope.launch {
                                try {
                                    val updatedTx = tx.copy(
                                        donorName = if (donorName.isNotBlank()) donorName else tx.donorName,
                                        amountPaise = if (amount.isNotBlank()) Money.fromRupees(amount) else tx.amountPaise,
                                        description = if (description.isNotBlank()) description else tx.description,
                                        paymentMode = paymentMode,
                                        itemName = if (itemName.isNotBlank()) itemName else tx.itemName,
                                        quantity = if (quantity.isNotBlank()) quantity else tx.quantity,
                                        edited = true,
                                        editedAt = Timestamp.now()
                                    )
                                    viewModel.saveTransaction(mandalId, year, updatedTx)
                                    onBack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Update failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Update Record")
                    }
                }

                if (existingTx?.type != TransactionType.OPENING_BALANCE) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    viewModel.deleteTransaction(mandalId, year, txId)
                                    onBack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Delete failed: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Record")
                    }
                }
            }
        }
    }
}
