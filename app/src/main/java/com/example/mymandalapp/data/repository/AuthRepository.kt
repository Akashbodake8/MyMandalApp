package com.example.mymandalapp.data.repository

import com.example.mymandalapp.core.mandal.FinancialYearConfig
import com.example.mymandalapp.core.mandal.MandalLookup
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    suspend fun isMandalIdAvailable(mandalId: String): Boolean {
        if (mandalId.isBlank()) return false
        val doc = firestore.collection("mandal_lookup")
            .document(mandalId.uppercase())
            .get()
            .await()
        return !doc.exists()
    }

    suspend fun signIn(identifier: String, password: String): Result<FirebaseUser> {
        val trimmedIdentifier = identifier.trim()
        return if (trimmedIdentifier.contains("@")) {
            signInWithEmail(trimmedIdentifier.lowercase(), password)
        } else {
            signInWithMandalId(trimmedIdentifier, password)
        }
    }

    private suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        android.util.Log.d("AuthRepo", "signInWithEmail started for: $email")
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Authentication failed"))
            android.util.Log.d("AuthRepo", "signInWithEmail success: ${user.uid}")
            Result.success(user)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            android.util.Log.w("AuthRepo", "signInWithEmail: User not found")
            Result.failure(Exception("No account found for this email."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            android.util.Log.w("AuthRepo", "signInWithEmail: Invalid credentials")
            Result.failure(Exception("Incorrect password. Please try again."))
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            android.util.Log.e("AuthRepo", "signInWithEmail: Network error", e)
            Result.failure(Exception("Network error. Please check your internet connection."))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "signInWithEmail: Unexpected error", e)
            Result.failure(Exception("Login error: ${e.localizedMessage}"))
        }
    }

    private suspend fun signInWithMandalId(mandalId: String, password: String): Result<FirebaseUser> {
        val mid = mandalId.uppercase()
        android.util.Log.d("AuthRepo", "signInWithMandalId started for: $mid")
        return try {
            // 1. Look up the email associated with the Mandal ID
            val doc = firestore.collection("mandal_lookup")
                .document(mid)
                .get()
                .await()
            
            if (!doc.exists()) {
                android.util.Log.w("AuthRepo", "signInWithMandalId: $mid not found in mandal_lookup")
                return Result.failure(Exception("Mandal ID not found. Please check and try again."))
            }
            
            val email = doc.getString("email") 
            if (email == null) {
                android.util.Log.e("AuthRepo", "signInWithMandalId: email field missing for $mid")
                return Result.failure(Exception("Invalid Mandal configuration. Please contact support."))
            }

            android.util.Log.d("AuthRepo", "signInWithMandalId: Resolved $mid to $email")
            // 2. Sign in with email and password
            signInWithEmail(email, password)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "signInWithMandalId error", e)
            Result.failure(e)
        }
    }

    suspend fun registerMandal(
        profile: MandalProfile,
        password: String,
        initialYear: String,
        openingBalancePaise: Long,
        startingReceiptCount: Long = 0L
    ): Result<FirebaseUser> {
        return try {
            val mandalId = profile.mandalId.uppercase().trim()
            val email = profile.treasurerEmail.lowercase().trim()

            if (email.isBlank()) {
                return Result.failure(Exception("Treasurer Email is required"))
            }

            // 1. Check if Mandal ID is already taken in lookup
            val lookupDoc = firestore.collection("mandal_lookup").document(mandalId).get().await()
            if (lookupDoc.exists()) {
                return Result.failure(Exception("Mandal ID already exists"))
            }

            // 2. Create Firebase User
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("User creation failed"))

            // 3. Create Mandal Profile, Lookup mapping, Year config, and Opening Balance in a batch
            val batch = firestore.batch()
            
            val updatedProfile = profile.copy(
                mandalId = mandalId,
                treasurerEmail = email,
                treasurerUid = user.uid
            )
            
            val mandalRef = firestore.collection("mandals").document(mandalId)
            val yearRef = mandalRef.collection("years").document(initialYear)
            
            val yearConfig = FinancialYearConfig(
                id = initialYear,
                yearName = "$initialYear Festival",
                openingBalancePaise = openingBalancePaise,
                isActive = true,
                createdBy = user.uid
            )

            val openingBalanceTransaction = Transaction(
                id = "OPENING_BALANCE",
                type = TransactionType.OPENING_BALANCE,
                amountPaise = openingBalancePaise,
                description = "Initial Opening Balance",
                createdBy = user.uid,
                createdAt = Timestamp.now()
            )

            val counterRef = yearRef.collection("config").document("counters")
            
            batch.set(mandalRef, updatedProfile)
            batch.set(firestore.collection("mandal_lookup").document(mandalId), MandalLookup(mandalId, email))
            batch.set(yearRef, yearConfig)
            batch.set(yearRef.collection("transactions").document("OPENING_BALANCE"), openingBalanceTransaction)
            batch.set(counterRef, mapOf("donation_count" to startingReceiptCount))
            
            batch.commit().await()
            
            Result.success(user)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Result.failure(Exception("This email address is already registered."))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "Registration error", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
    
    suspend fun getMandalId(): String {
        val user = currentUser.value ?: return "GUEST"
        val email = user.email?.lowercase()?.trim() ?: return "GUEST"
        
        // Find mandal ID from lookup where email matches
        val query = firestore.collection("mandal_lookup")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
            
        return if (!query.isEmpty) {
            query.documents[0].id
        } else {
            "GUEST"
        }
    }
}
