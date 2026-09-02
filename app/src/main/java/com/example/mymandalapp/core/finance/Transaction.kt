package com.example.mymandalapp.core.finance

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

enum class TransactionType {
    INCOME, EXPENSE, OPENING_BALANCE, OBJECT_DONATION
}

enum class PaymentMode {
    CASH, UPI
}

@IgnoreExtraProperties
data class Transaction(
    val id: String = "",
    val type: TransactionType = TransactionType.INCOME,
    val amountPaise: Long = 0L,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val date: Timestamp = Timestamp.now(),
    val description: String = "",
    
    @get:PropertyName("edited")
    @set:PropertyName("edited")
    var edited: Boolean = false,
    
    // Donation Specific
    val donorName: String? = null,
    val donorMobile: String? = null,
    val donorAddress: String? = null,
    val donationPurpose: String? = null,
    val receiptNumber: String? = null,
    
    // Expense Specific
    val expenseCategory: String? = null,
    val vendorName: String? = null,
    val billUrl: String? = null,
    
    // Object Donation Specific
    val itemName: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val estimatedValuePaise: Long? = null,
    
    // Metadata
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val editedAt: Timestamp? = null
)
