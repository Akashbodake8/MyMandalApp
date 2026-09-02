package com.example.mymandalapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
fun AddObjectDonationScreen(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = viewModel(),
    mandalId: String = "GUEST",
    year: String = "2026"
) {
    var donorName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Kg") }
    var estimatedValue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Error States
    var nameError by remember { mutableStateOf<String?>(null) }
    var itemError by remember { mutableStateOf<String?>(null) }
    var qtyError by remember { mutableStateOf<String?>(null) }
    var estValueError by remember { mutableStateOf<String?>(null) }
    
    val units = listOf("Kg", "Gram", "Litre", "Piece", "Box", "Packet", "Bag", "Other")
    var expanded by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                        Text("Add Object Donation")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = donorName,
                onValueChange = { donorName = it; nameError = null },
                label = { Text("Donor Name *") },
                isError = nameError != null,
                supportingText = { if (nameError != null) Text(nameError!!) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it; itemError = null },
                label = { Text("Object / Item *") },
                isError = itemError != null,
                supportingText = { if (itemError != null) Text(itemError!!) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; qtyError = null },
                    label = { Text("Quantity *") },
                    isError = qtyError != null,
                    supportingText = { if (qtyError != null) Text(qtyError!!) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!state.isSaving) expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !state.isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    unit = u
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = estimatedValue,
                onValueChange = { estimatedValue = it; estValueError = null },
                label = { Text("Estimated Value (₹) - Optional") },
                isError = estValueError != null,
                supportingText = { if (estValueError != null) Text(estValueError!!) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            Spacer(Modifier.height(8.dp))
            
            Text(
                "Note: Object donations do not affect the monetary cash/UPI balance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    var hasError = false
                    if (!ValidationUtils.isNotEmpty(donorName)) { nameError = "Please enter donor name."; hasError = true }
                    if (!ValidationUtils.isNotEmpty(itemName)) { itemError = "Please enter item name."; hasError = true }
                    if (!ValidationUtils.isValidQuantity(quantity)) { qtyError = "Enter valid quantity."; hasError = true }
                    if (estimatedValue.isNotEmpty() && !ValidationUtils.isValidAmount(estimatedValue)) { estValueError = "Enter valid amount."; hasError = true }

                    if (!hasError) {
                        scope.launch {
                            try {
                                val transaction = Transaction(
                                    type = TransactionType.OBJECT_DONATION,
                                    donorName = ValidationUtils.trim(donorName),
                                    itemName = ValidationUtils.trim(itemName),
                                    quantity = ValidationUtils.trim(quantity),
                                    unit = unit,
                                    estimatedValuePaise = if (estimatedValue.isBlank()) null else Money.fromRupees(estimatedValue),
                                    description = ValidationUtils.trim(description),
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
                    Text("Save Object Donation")
                }
            }
        }
    }
}
