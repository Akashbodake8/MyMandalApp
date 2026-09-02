package com.example.mymandalapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymandalapp.core.finance.FinanceCalculator
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.data.repository.FirebaseRepository
import com.example.mymandalapp.data.repository.LocalBrandingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class DashboardState(
    val profile: MandalProfile = MandalProfile(),
    val mandalName: String = "",
    val mandalLocation: String = "",
    val hasLocalLogo: Boolean = false,
    val localLogoPath: String? = null,
    val hasLocalStamp: Boolean = false,
    val localStampPath: String? = null,
    val openingBalance: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val currentBalance: Long = 0L,
    val cashDonations: Long = 0L,
    val upiDonations: Long = 0L,
    val cashExpenses: Long = 0L,
    val upiExpenses: Long = 0L,
    val objectDonationCount: Int = 0,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

class FinanceViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
    private var localBrandingRepository: LocalBrandingRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    fun initLocalBrandingRepository(repo: LocalBrandingRepository) {
        this.localBrandingRepository = repo
        refreshLocalBrandingState()
    }

    private fun refreshLocalBrandingState() {
        localBrandingRepository?.let { repo ->
            _uiState.update { 
                it.copy(
                    hasLocalLogo = repo.hasBranding("logo"),
                    localLogoPath = repo.getBrandingPath("logo"),
                    hasLocalStamp = repo.hasBranding("stamp"),
                    localStampPath = repo.getBrandingPath("stamp")
                )
            }
        }
    }

    private var currentMandalId: String? = null
    private var currentYear: String? = null
    private var transactionJob: kotlinx.coroutines.Job? = null

    fun loadData(mandalId: String, year: String) {
        if (currentMandalId == mandalId && currentYear == year) {
            android.util.Log.d("FinanceVM", "loadData: Already active for $mandalId, $year")
            return
        }

        currentMandalId = mandalId
        currentYear = year
        transactionJob?.cancel()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        android.util.Log.d("FinanceVM", "loadData started: mandalId=$mandalId, year=$year, authUid=$uid")
        
        if (mandalId.isBlank() || mandalId == "GUEST") {
            android.util.Log.w("FinanceVM", "loadData aborted: Invalid mandalId")
            _uiState.update { it.copy(isLoading = false, error = "Invalid Mandal ID") }
            return
        }

        refreshLocalBrandingState()

        transactionJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Initial metadata fetch with timeout
                val success = withTimeoutOrNull(10000L) {
                    android.util.Log.d("FinanceVM", "Fetching profile and opening balance...")
                    val profile = repository.getMandalProfile(mandalId)
                    val openingBalance = repository.getOpeningBalance(mandalId, year)
                    
                    _uiState.update { 
                        it.copy(
                            profile = profile,
                            mandalName = profile.mandalName,
                            mandalLocation = profile.location,
                            openingBalance = openingBalance
                        )
                    }
                    true
                }

                if (success == null) {
                    android.util.Log.e("FinanceVM", "Timeout fetching mandal metadata")
                    _uiState.update { it.copy(isLoading = false, error = "Connection timeout. Check your internet connection.") }
                    return@launch
                }

                android.util.Log.d("FinanceVM", "Starting transaction flow for path: mandals/$mandalId/years/$year/transactions")
                repository.getTransactions(mandalId, year)
                    .onEach { transactions ->
                        android.util.Log.d("FinanceVM", "Received ${transactions.size} transactions")
                        val income = FinanceCalculator.calculateTotalIncome(transactions)
                        val expense = FinanceCalculator.calculateTotalExpenses(transactions)
                        val balance = FinanceCalculator.calculateCurrentBalance(_uiState.value.openingBalance, transactions)
                        
                        val incomeByMode = FinanceCalculator.calculateIncomeByPaymentMode(transactions)
                        val expenseByMode = FinanceCalculator.calculateExpenseByPaymentMode(transactions)
                        val objectCount = FinanceCalculator.calculateObjectDonationCount(transactions)
                        
                        _uiState.update { 
                            it.copy(
                                totalIncome = income,
                                totalExpense = expense,
                                currentBalance = balance,
                                cashDonations = incomeByMode[com.example.mymandalapp.core.finance.PaymentMode.CASH] ?: 0L,
                                upiDonations = incomeByMode[com.example.mymandalapp.core.finance.PaymentMode.UPI] ?: 0L,
                                cashExpenses = expenseByMode[com.example.mymandalapp.core.finance.PaymentMode.CASH] ?: 0L,
                                upiExpenses = expenseByMode[com.example.mymandalapp.core.finance.PaymentMode.UPI] ?: 0L,
                                objectDonationCount = objectCount,
                                transactions = transactions,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .catch { e ->
                        if (e !is kotlinx.coroutines.CancellationException) {
                            android.util.Log.e("FinanceVM", "Transaction flow error", e)
                            _uiState.update { it.copy(isLoading = false, error = "Failed to load transactions: ${e.message}") }
                        }
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("FinanceVM", "loadData top-level error", e)
                    _uiState.update { it.copy(isLoading = false, error = "System Error: ${e.message}") }
                }
            }
        }
    }

    fun saveLocalBranding(uri: Uri, type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = localBrandingRepository?.saveBranding(uri, type) ?: false
            if (success) {
                _uiState.update { it.copy(isSaving = false) }
                refreshLocalBrandingState()
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save $type locally") }
            }
        }
    }

    fun deleteLocalBranding(type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = localBrandingRepository?.deleteBranding(type) ?: true
            if (success) {
                _uiState.update { it.copy(isSaving = false) }
                refreshLocalBrandingState()
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Failed to delete $type") }
            }
        }
    }

    fun updateProfile(mandalId: String, profile: MandalProfile, onComplete: () -> Unit) {
        android.util.Log.d("FinanceVM", "updateProfile started for $mandalId")
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                android.util.Log.d("FinanceVM", "Updating Firestore profile...")
                repository.updateMandalProfile(profile)
                _uiState.update { it.copy(isSaving = false, profile = profile, mandalName = profile.mandalName, mandalLocation = profile.location) }
                android.util.Log.d("FinanceVM", "Profile updated successfully")
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("FinanceVM", "Error updating profile", e)
                _uiState.update { it.copy(isSaving = false, error = "Unable to save profile: ${e.message}") }
            }
        }
    }

    fun removeBranding(mandalId: String, type: String, onComplete: () -> Unit) {
        // This method is now legacy as branding is local, but kept for compatibility if needed.
        // It now just calls deleteLocalBranding.
        deleteLocalBranding(type)
        onComplete()
    }

    private val _nextReceiptNo = MutableStateFlow<String?>(null)
    val nextReceiptNo: StateFlow<String?> = _nextReceiptNo.asStateFlow()

    fun fetchNextReceiptNo(mandalId: String, year: String) {
        viewModelScope.launch {
            try {
                _nextReceiptNo.value = repository.getExpectedNextReceiptNumber(mandalId, year)
            } catch (e: Exception) {
                _nextReceiptNo.value = null
            }
        }
    }

    suspend fun saveDonation(mandalId: String, year: String, transaction: Transaction) {
        _uiState.update { it.copy(isSaving = true) }
        try {
            repository.addDonation(mandalId, year, transaction)
            _uiState.update { it.copy(isSaving = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = e.message) }
            throw e
        }
    }

    suspend fun saveTransaction(mandalId: String, year: String, transaction: Transaction) {
        _uiState.update { it.copy(isSaving = true) }
        try {
            repository.addTransaction(mandalId, year, transaction)
            _uiState.update { it.copy(isSaving = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = e.message) }
            throw e
        }
    }

    suspend fun deleteTransaction(mandalId: String, year: String, txId: String) {
        _uiState.update { it.copy(isSaving = true) }
        try {
            repository.deleteTransaction(mandalId, year, txId)
            _uiState.update { it.copy(isSaving = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = e.message) }
            throw e
        }
    }
}
