package com.example.mymandalapp.core.mandal

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MandalProfile(
    val mandalId: String = "",
    val mandalName: String = "",
    val location: String = "",
    val village: String = "",
    val area: String = "",
    val fullAddress: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val treasurerName: String = "",
    val treasurerContact: String = "",
    val treasurerEmail: String = "",
    val treasurerUid: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    
    // Default location settings for donors
    val defaultVillage: String = "",
    val defaultArea: String = ""
)

data class FinancialYearConfig(
    val id: String = "", // e.g., "2026"
    val yearName: String = "", // e.g., "2026 Festival"
    val openingBalancePaise: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val createdBy: String = ""
)

data class MandalLookup(
    val mandalId: String = "",
    val email: String = ""
)
