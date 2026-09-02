package com.example.mymandalapp.data.repository

import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.core.finance.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.net.Uri

class FirebaseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Get transactions for a specific mandal and year.
     */
    fun getTransactions(mandalId: String, year: String): Flow<List<Transaction>> = callbackFlow {
        val registration = firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()
                trySend(transactions)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getMandalProfile(mandalId: String): MandalProfile {
        android.util.Log.d("FirebaseRepo", "getMandalProfile for $mandalId")
        return firestore.collection("mandals")
            .document(mandalId)
            .get()
            .await()
            .toObject(MandalProfile::class.java) ?: MandalProfile()
    }

    suspend fun updateMandalProfile(profile: MandalProfile) {
        android.util.Log.d("FirebaseRepo", "updateMandalProfile for ${profile.mandalId}")
        firestore.collection("mandals")
            .document(profile.mandalId)
            .set(profile)
            .await()
    }

    suspend fun getExpectedNextReceiptNumber(mandalId: String, year: String): String {
        val counterRef = firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("config")
            .document("counters")

        val snapshot = counterRef.get().await()
        val nextNumber = (snapshot.getLong("donation_count") ?: 0L) + 1
        
        val prefix = if (mandalId.length >= 3) mandalId.take(3).uppercase() else mandalId.uppercase()
        return "$prefix-$year-${nextNumber.toString().padStart(4, '0')}"
    }


    suspend fun addDonation(mandalId: String, year: String, transaction: Transaction) {
        val counterRef = firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("config")
            .document("counters")

        val txnsCollection = firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("transactions")

        firestore.runTransaction { firestoreTx ->
            // 1. Get and increment counter
            val snapshot = firestoreTx.get(counterRef)
            val nextNumber = (snapshot.getLong("donation_count") ?: 0L) + 1
            firestoreTx.set(counterRef, mapOf("donation_count" to nextNumber))

            // 2. Generate Receipt Number
            val prefix = if (mandalId.length >= 3) mandalId.take(3).uppercase() else mandalId.uppercase()
            val receiptNumber = "$prefix-$year-${nextNumber.toString().padStart(4, '0')}"

            // 3. Prepare Transaction Document
            val docRef = if (transaction.id.isNotEmpty()) {
                txnsCollection.document(transaction.id)
            } else {
                txnsCollection.document()
            }
            val txnWithIdAndReceipt = transaction.copy(
                id = if (transaction.id.isEmpty()) docRef.id else transaction.id,
                receiptNumber = receiptNumber
            )

            // 4. Write Transaction
            firestoreTx.set(docRef, txnWithIdAndReceipt)
        }.await()
    }

    suspend fun addTransaction(mandalId: String, year: String, transaction: Transaction) {
        val collection = firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("transactions")
        
        val docRef = if (transaction.id.isNotEmpty()) {
            collection.document(transaction.id)
        } else {
            collection.document()
        }
        
        val txnWithId = if (transaction.id.isEmpty()) transaction.copy(id = docRef.id) else transaction
        docRef.set(txnWithId).await()
    }

    suspend fun updateTransaction(mandalId: String, year: String, transaction: Transaction) {
        firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("transactions")
            .document(transaction.id)
            .set(transaction)
            .await()
    }

    suspend fun getTransaction(mandalId: String, year: String, txId: String): Transaction? {
        return firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("transactions")
            .document(txId)
            .get()
            .await()
            .toObject(Transaction::class.java)
    }

    suspend fun deleteTransaction(mandalId: String, year: String, txId: String) {
        firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .collection("transactions")
            .document(txId)
            .delete()
            .await()
    }

    suspend fun getOpeningBalance(mandalId: String, year: String): Long {
        val doc = firestore.collection("mandals")
            .document(mandalId)
            .collection("years")
            .document(year)
            .get()
            .await()
        return doc.getLong("openingBalancePaise") ?: 0L
    }
}
