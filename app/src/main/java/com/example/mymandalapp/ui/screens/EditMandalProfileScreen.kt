package com.example.mymandalapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mymandalapp.data.repository.LocalBrandingRepository
import com.example.mymandalapp.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMandalProfileScreen(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = viewModel(),
    mandalId: String = "GUEST"
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(mandalId) {
        viewModel.initLocalBrandingRepository(LocalBrandingRepository(context))
        if (mandalId != "GUEST") {
            viewModel.loadData(mandalId, "2026")
        }
    }

    // Form States
    var mandalName by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.mandalName) }
    var location by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.location) }
    var village by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.village) }
    var area by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.area) }
    var fullAddress by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.fullAddress) }
    var contactNumber by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.contactNumber) }
    var email by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.email) }
    
    var treasurerName by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.treasurerName) }
    var treasurerContact by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.treasurerContact) }
    var treasurerEmail by remember(profile.mandalId, state.isLoading) { mutableStateOf(profile.treasurerEmail) }

    // Logo Branding
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveLocalBranding(it, "logo") }
    }

    // Stamp Branding
    val stampLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveLocalBranding(it, "stamp") }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Mandal Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    state.error?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        Button(onClick = { viewModel.loadData(mandalId, "2026") }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Mandal Details
                EditSection(title = "MANDAL DETAILS") {
                    OutlinedTextField(value = mandalName, onValueChange = { mandalName = it }, label = { Text("Mandal Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village *") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area / Locality") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = fullAddress, onValueChange = { fullAddress = it }, label = { Text("Full Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("Contact Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Mandal Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                }

                // 2. Treasurer Details
                EditSection(title = "TREASURER DETAILS") {
                    OutlinedTextField(value = treasurerName, onValueChange = { treasurerName = it }, label = { Text("Treasurer Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = treasurerContact, onValueChange = { treasurerContact = it }, label = { Text("Treasurer Mobile *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = treasurerEmail, onValueChange = { treasurerEmail = it }, label = { Text("Treasurer Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), enabled = false)
                }

                // 3. Branding
                EditSection(title = "MANDAL BRANDING (Local Device Only)") {
                    BrandingEditItem(
                        label = "Mandal Logo",
                        currentSource = state.localLogoPath,
                        onPick = { logoLauncher.launch("image/*") },
                        onRemove = { viewModel.deleteLocalBranding("logo") }
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BrandingEditItem(
                        label = "Mandal Stamp",
                        currentSource = state.localStampPath,
                        onPick = { stampLauncher.launch("image/*") },
                        onRemove = { viewModel.deleteLocalBranding("stamp") }
                    )
                }

                Button(
                    onClick = {
                        android.util.Log.d("EditProfile", "Save button clicked")
                        if (mandalName.isBlank() || village.isBlank() || treasurerName.isBlank() || treasurerContact.isBlank()) {
                            android.util.Log.w("EditProfile", "Validation failed")
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill all required fields (*)")
                            }
                        } else {
                            android.util.Log.d("EditProfile", "Validation passed")
                            val updatedProfile = profile.copy(
                                mandalName = mandalName.trim(),
                                location = location.trim(),
                                village = village.trim(),
                                area = area.trim(),
                                fullAddress = fullAddress.trim(),
                                contactNumber = contactNumber.trim(),
                                email = email.trim(),
                                treasurerName = treasurerName.trim(),
                                treasurerContact = treasurerContact.trim()
                            )
                            viewModel.updateProfile(mandalId, updatedProfile) {
                                android.util.Log.d("EditProfile", "Success, going back")
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Profile Changes")
                    }
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        content()
        HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
    }
}

@Composable
fun BrandingEditItem(
    label: String,
    currentSource: Any?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                if (currentSource != null) {
                    AsyncImage(model = currentSource, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("None", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                    }
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                    Text(if (currentSource == null) "Upload" else "Change")
                }
                if (currentSource != null) {
                    TextButton(onClick = onRemove, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}
