package com.example.mymandalapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.mymandalapp.ui.viewmodel.AuthState
import com.example.mymandalapp.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var identifierError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetAuthState() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_ganpati_emblem),
            contentDescription = "Mandal Logo",
            modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
        )
        
        Text(
            "My Mandal",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Finance Management",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it; identifierError = null },
            label = { Text("Mandal ID or Email") },
            isError = identifierError != null,
            supportingText = { if (identifierError != null) Text(identifierError!!) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = authState !is AuthState.Loading
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("Password") },
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError!!) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = authState !is AuthState.Loading
        )

        Spacer(Modifier.height(24.dp))

        if (authState is AuthState.Error) {
            Text(
                (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                var hasError = false
                if (identifier.isBlank()) { identifierError = "Please enter Mandal ID or Email."; hasError = true }
                if (password.isBlank()) { passwordError = "Please enter password."; hasError = true }
                
                if (!hasError) {
                    viewModel.login(identifier.trim(), password, onLoginSuccess)
                }
            },
            enabled = authState !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Login")
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister, enabled = authState !is AuthState.Loading) {
            Text("Register New Mandal")
        }
    }
}
