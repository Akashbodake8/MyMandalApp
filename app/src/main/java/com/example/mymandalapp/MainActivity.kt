package com.example.mymandalapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mymandalapp.ui.screens.*
import com.example.mymandalapp.ui.theme.MyMandalAppTheme
import com.example.mymandalapp.ui.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Firebase Connection
        try {
            val firebaseApp = FirebaseApp.getInstance()
            val projectId = firebaseApp.options.projectId
            Log.d("FirebaseCheck", "Firebase initialized successfully with Project ID: $projectId")
        } catch (e: Exception) {
            Log.e("FirebaseCheck", "Firebase initialization failed: ${e.message}")
        }

        enableEdgeToEdge()
        setContent {
            MyMandalAppTheme {
                val authViewModel: AuthViewModel = viewModel()
                val user by authViewModel.currentUser.collectAsStateWithLifecycle()
                val mandalId by authViewModel.mandalId.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                
                val startDestination = if (user == null) "login" else "dashboard"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("registration")
                            }
                        )
                    }
                    composable("registration") {
                        RegistrationScreen(
                            onBack = { navController.popBackStack() },
                            onSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            mandalId = mandalId,
                            onAddDonation = { navController.navigate("add_donation") },
                            onAddExpense = { navController.navigate("add_expense") },
                            onAddObjectDonation = { navController.navigate("add_object_donation") },
                            onEditTransaction = { tx ->
                                navController.navigate("edit_transaction/${tx.id}")
                            },
                            onNavigateToSettings = {
                                navController.navigate("mandal_profile")
                            },
                            onLogout = { 
                                navController.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("mandal_profile") {
                        MandalProfileScreen(
                            mandalId = mandalId,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("edit_mandal_profile") }
                        )
                    }
                    composable("edit_mandal_profile") {
                        EditMandalProfileScreen(
                            mandalId = mandalId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("add_donation") {
                        AddDonationScreen(
                            mandalId = mandalId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("add_object_donation") {
                        AddObjectDonationScreen(
                            mandalId = mandalId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("add_expense") {
                        AddExpenseScreen(
                            mandalId = mandalId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("edit_transaction/{txId}") { backStackEntry ->
                        val txId = backStackEntry.arguments?.getString("txId") ?: ""
                        EditTransactionScreen(
                            txId = txId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
