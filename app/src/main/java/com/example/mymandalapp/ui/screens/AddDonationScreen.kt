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
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.finance.PaymentMode
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import com.example.mymandalapp.ui.viewmodel.FinanceViewModel
import com.example.mymandalapp.core.utils.ValidationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDonationScreen(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = viewModel(),
    mandalId: String = "GUEST",
    year: String = "2026"
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var donorName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var village by remember(state.profile) { 
        mutableStateOf(state.profile.defaultVillage.ifEmpty { state.mandalLocation.split(",").firstOrNull()?.trim() ?: "" }) 
    }
    var area by remember(state.profile) { 
        mutableStateOf(state.profile.defaultArea.ifEmpty { state.mandalLocation.split(",").lastOrNull()?.trim() ?: "" }) 
    }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    
    // Error States
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var villageError by remember { mutableStateOf<String?>(null) }
    
    val nextReceiptNo by viewModel.nextReceiptNo.collectAsState()

    LaunchedEffect(mandalId, year) {
        viewModel.fetchNextReceiptNo(mandalId, year)
    }
    
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
                        Column {
                            Text("Add Donation", style = MaterialTheme.typography.titleMedium)
                            nextReceiptNo?.let {
                                Text("Next Receipt: $it", style = MaterialTheme.typography.labelSmall)
                            }
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            OutlinedTextField(
                value = mobile,
                onValueChange = { if (it.length <= 10) mobile = it; mobileError = null },
                label = { Text("Mobile Number (Optional)") },
                isError = mobileError != null,
                supportingText = { if (mobileError != null) Text(mobileError!!) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it; villageError = null },
                    label = { Text("Village *") },
                    isError = villageError != null,
                    supportingText = { if (villageError != null) Text(villageError!!) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area / Locality") },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                )
            }

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
                    if (!ValidationUtils.isNotEmpty(donorName)) { nameError = "Please enter donor name."; hasError = true }
                    if (!ValidationUtils.isValidAmount(amount)) { amountError = "Amount must be greater than ₹0."; hasError = true }
                    if (!ValidationUtils.isValidMobile(mobile)) { mobileError = "Mobile number must be 10 digits."; hasError = true }
                    if (!ValidationUtils.isNotEmpty(village)) { villageError = "Village is required."; hasError = true }

                    if (!hasError) {
                        scope.launch {
                            try {
                                val transaction = Transaction(
                                    type = TransactionType.INCOME,
                                    amountPaise = Money.fromRupees(amount),
                                    donorName = ValidationUtils.trim(donorName),
                                    donorMobile = ValidationUtils.trim(mobile),
                                    donorAddress = "${ValidationUtils.trim(village)}, ${ValidationUtils.trim(area)}",
                                    paymentMode = paymentMode
                                )
                                viewModel.saveDonation(mandalId, year, transaction)
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
                    Text("Save Donation")
                }
            }
        }
    }
}

@Composable
fun PaymentModeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(label)
    }
}
