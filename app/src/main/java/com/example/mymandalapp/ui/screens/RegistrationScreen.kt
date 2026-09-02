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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.core.utils.ValidationUtils
import com.example.mymandalapp.ui.viewmodel.AuthState
import com.example.mymandalapp.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var currentStep by remember { mutableIntStateOf(1) }
    
    // Step 1: Mandal Info
    var mandalName by remember { mutableStateOf("") }
    var mandalId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mandalContact by remember { mutableStateOf("") }
    
    // Step 2: Treasurer Info
    var treasurerName by remember { mutableStateOf("") }
    var treasurerContact by remember { mutableStateOf("") }
    var treasurerEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Step 3: Financial Year Info
    var yearName by remember { mutableStateOf("2026 Festival") }
    var openingBalance by remember { mutableStateOf("") }
    var startingReceiptNo by remember { mutableStateOf("") }

    // Error States
    var mandalNameError by remember { mutableStateOf<String?>(null) }
    var mandalIdError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var mandalContactError by remember { mutableStateOf<String?>(null) }
    
    var treasurerNameError by remember { mutableStateOf<String?>(null) }
    var treasurerContactError by remember { mutableStateOf<String?>(null) }
    var treasurerEmailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    var yearNameError by remember { mutableStateOf<String?>(null) }
    var openingBalanceError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    
    DisposableEffect(Unit) {
        onDispose { viewModel.resetAuthState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mandal Registration") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = authState !is AuthState.Loading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ganpati_emblem),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).padding(end = 12.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            when (currentStep) {
                1 -> MandalInfoStep(
                    mandalName, { mandalName = it; mandalNameError = null }, mandalNameError,
                    mandalId, { mandalId = it; mandalIdError = null }, mandalIdError,
                    location, { location = it; locationError = null }, locationError,
                    mandalContact, { mandalContact = it; mandalContactError = null }, mandalContactError
                )
                2 -> TreasurerInfoStep(
                    treasurerName, { treasurerName = it; treasurerNameError = null }, treasurerNameError,
                    treasurerContact, { treasurerContact = it; treasurerContactError = null }, treasurerContactError,
                    treasurerEmail, { treasurerEmail = it; treasurerEmailError = null }, treasurerEmailError,
                    password, { password = it; passwordError = null }, passwordError,
                    confirmPassword, { confirmPassword = it; confirmPasswordError = null }, confirmPasswordError
                )
                3 -> FinancialYearStep(
                    yearName, { yearName = it; yearNameError = null }, yearNameError,
                    openingBalance, { openingBalance = it; openingBalanceError = null }, openingBalanceError,
                    startingReceiptNo, { startingReceiptNo = it }
                )
            }

            Spacer(Modifier.height(32.dp))

            if (authState is AuthState.Error) {
                Text(
                    (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = authState !is AuthState.Loading
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            1 -> {
                                var hasError = false
                                if (!ValidationUtils.isNotEmpty(mandalName)) { mandalNameError = "Please enter mandal name."; hasError = true }
                                if (!ValidationUtils.isValidMandalId(mandalId)) { mandalIdError = "Invalid Mandal ID (min 4 chars, no spaces)."; hasError = true }
                                if (!ValidationUtils.isNotEmpty(location)) { locationError = "Please enter location."; hasError = true }
                                if (mandalContact.isNotEmpty() && !ValidationUtils.isValidMobile(mandalContact)) { mandalContactError = "Mobile number must be 10 digits."; hasError = true }
                                
                                if (!hasError) {
                                    viewModel.checkMandalIdAvailability(mandalId) { isAvailable ->
                                        if (isAvailable) {
                                            currentStep = 2
                                        } else {
                                            mandalIdError = "This Mandal ID is already in use."
                                        }
                                    }
                                }
                            }
                            2 -> {
                                var hasError = false
                                if (!ValidationUtils.isNotEmpty(treasurerName)) { treasurerNameError = "Please enter treasurer name."; hasError = true }
                                if (!ValidationUtils.isValidMobile(treasurerContact)) { treasurerContactError = "Mobile number must be 10 digits."; hasError = true }
                                if (!ValidationUtils.isValidEmail(treasurerEmail)) { treasurerEmailError = "Enter a valid email."; hasError = true }
                                val pError = ValidationUtils.validatePassword(password)
                                if (pError != null) { passwordError = pError; hasError = true }
                                if (password != confirmPassword) { confirmPasswordError = "Passwords do not match."; hasError = true }
                                
                                if (!hasError) currentStep = 3
                            }
                            3 -> {
                                var hasError = false
                                if (!ValidationUtils.isNotEmpty(yearName)) { yearNameError = "Please enter year name."; hasError = true }
                                if (!ValidationUtils.isValidAmount(openingBalance)) { openingBalanceError = "Enter valid amount."; hasError = true }
                                
                                if (!hasError) {
                                    val profile = MandalProfile(
                                        mandalId = ValidationUtils.trim(mandalId).uppercase(),
                                        mandalName = ValidationUtils.trim(mandalName),
                                        location = ValidationUtils.trim(location),
                                        village = ValidationUtils.trim(location),
                                        fullAddress = ValidationUtils.trim(location),
                                        contactNumber = ValidationUtils.trim(mandalContact),
                                        treasurerName = ValidationUtils.trim(treasurerName),
                                        treasurerContact = ValidationUtils.trim(treasurerContact),
                                        treasurerEmail = ValidationUtils.trim(treasurerEmail).lowercase(),
                                        defaultVillage = ValidationUtils.trim(location)
                                    )
                                    
                                    val selectedYear = yearName.filter { it.isDigit() }.take(4).ifEmpty { "2026" }
                                    
                                    viewModel.register(
                                        profile = profile,
                                        password = password,
                                        initialYear = selectedYear,
                                        openingBalancePaise = Money.fromRupees(openingBalance),
                                        startingReceiptCount = startingReceiptNo.toLongOrNull() ?: 0L,
                                        onSuccess = onSuccess
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (currentStep < 3) "Next" else "Register & Setup")
                    }
                }
            }
        }
    }
}

@Composable
fun MandalInfoStep(
    name: String, onNameChange: (String) -> Unit, nameError: String?,
    id: String, onIdChange: (String) -> Unit, idError: String?,
    loc: String, onLocChange: (String) -> Unit, locError: String?,
    contact: String, onContactChange: (String) -> Unit, contactError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mandal Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Mandal Name *") }, isError = nameError != null, supportingText = { if (nameError != null) Text(nameError) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = id, onValueChange = onIdChange, label = { Text("Mandal ID * (e.g. BGMV2026)") }, isError = idError != null, supportingText = { if (idError != null) Text(idError) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = loc, onValueChange = onLocChange, label = { Text("Location/Address *") }, isError = locError != null, supportingText = { if (locError != null) Text(locError) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contact, onValueChange = onContactChange, label = { Text("Mandal Contact (Optional)") }, isError = contactError != null, supportingText = { if (contactError != null) Text(contactError) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun TreasurerInfoStep(
    name: String, onNameChange: (String) -> Unit, nameError: String?,
    contact: String, onContactChange: (String) -> Unit, contactError: String?,
    email: String, onEmailChange: (String) -> Unit, emailError: String?,
    pass: String, onPassChange: (String) -> Unit, passError: String?,
    confirm: String, onConfirmChange: (String) -> Unit, confirmError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Treasurer Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Treasurer Name *") }, isError = nameError != null, supportingText = { if (nameError != null) Text(nameError) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contact, onValueChange = onContactChange, label = { Text("Treasurer Mobile *") }, isError = contactError != null, supportingText = { if (contactError != null) Text(contactError) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("Treasurer Email *") }, isError = emailError != null, supportingText = { if (emailError != null) Text(emailError) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = onPassChange, label = { Text("Create Password *") }, isError = passError != null, supportingText = { if (passError != null) Text(passError) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = confirm, onValueChange = onConfirmChange, label = { Text("Confirm Password *") }, isError = confirmError != null, supportingText = { if (confirmError != null) Text(confirmError) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun FinancialYearStep(
    name: String, onNameChange: (String) -> Unit, nameError: String?,
    balance: String, onBalanceChange: (String) -> Unit, balanceError: String?,
    receiptNo: String, onReceiptNoChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Initial Year Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Financial Year Name *") }, isError = nameError != null, supportingText = { if (nameError != null) Text(nameError) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = balance, onValueChange = onBalanceChange, label = { Text("Opening Balance (₹) *") }, isError = balanceError != null, supportingText = { if (balanceError != null) Text(balanceError) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = receiptNo, onValueChange = onReceiptNoChange, label = { Text("Starting Receipt No (Optional)") }, placeholder = { Text("e.g. 100") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Text("If you are migrating from paper receipts, enter the last receipt number used.", style = MaterialTheme.typography.bodySmall)
    }
}
