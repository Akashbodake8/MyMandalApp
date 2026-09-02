package com.example.mymandalapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = repository.currentUser
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mandalId: StateFlow<String> = currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf("GUEST")
            } else {
                flow {
                    try {
                        val id = repository.getMandalId()
                        emit(id)
                    } catch (e: Exception) {
                        android.util.Log.e("AuthVM", "Failed to get Mandal ID: ${e.message}")
                        emit("GUEST")
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "GUEST"
        )

    init {
        // No manual management of _mandalId needed now
    }

    fun checkMandalIdAvailability(mandalId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(repository.isMandalIdAvailable(mandalId))
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun login(identifier: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signIn(identifier, password)
            result.onSuccess {
                _authState.value = AuthState.Success
                onSuccess()
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Login Failed")
            }
        }
    }

    fun register(
        profile: MandalProfile,
        password: String,
        initialYear: String,
        openingBalancePaise: Long,
        startingReceiptCount: Long = 0L,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.registerMandal(profile, password, initialYear, openingBalancePaise, startingReceiptCount)
            result.onSuccess {
                _authState.value = AuthState.Success
                onSuccess()
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Registration Failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            _authState.value = AuthState.Idle
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
