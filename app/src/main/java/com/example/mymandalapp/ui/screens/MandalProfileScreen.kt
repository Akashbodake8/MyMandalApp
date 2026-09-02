package com.example.mymandalapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.data.repository.LocalBrandingRepository
import com.example.mymandalapp.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandalProfileScreen(
    mandalId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: FinanceViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val context = LocalContext.current

    LaunchedEffect(mandalId) {
        viewModel.initLocalBrandingRepository(LocalBrandingRepository(context))
        if (mandalId != "GUEST") {
            viewModel.loadData(mandalId, "2026")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mandal Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Profile Header
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.hasLocalLogo && state.localLogoPath != null) {
                        AsyncImage(
                            model = state.localLogoPath,
                            contentDescription = "Mandal Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ganpati_emblem),
                            contentDescription = "Default Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    profile.mandalName.ifEmpty { "Mandal Name Not Set" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    "Mandal ID: ${profile.mandalId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider()

                // Details Sections
                ProfileSection(title = "MANDAL DETAILS") {
                    ProfileRow("Location", profile.location)
                    ProfileRow("Village", profile.village)
                    ProfileRow("Area / Locality", profile.area)
                    ProfileRow("Full Address", profile.fullAddress)
                    ProfileRow("Contact", profile.contactNumber)
                    ProfileRow("Email", profile.email)
                }

                ProfileSection(title = "TREASURER") {
                    ProfileRow("Name", profile.treasurerName)
                    ProfileRow("Mobile", profile.treasurerContact)
                    ProfileRow("Email", profile.treasurerEmail)
                }

                ProfileSection(title = "MANDAL BRANDING (Local Only)") {
                    BrandingPreview("Mandal Logo", state.localLogoPath)
                    BrandingPreview("Mandal Stamp", state.localStampPath)
                }

                ProfileSection(title = "FINANCIAL YEAR") {
                    ProfileRow("Year", "2026")
                    ProfileRow("Opening Balance", Money.format(state.openingBalance))
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BrandingPreview(label: String, source: Any?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (source != null) {
            AsyncImage(
                model = source,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text("Not Uploaded", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
        }
    }
}
