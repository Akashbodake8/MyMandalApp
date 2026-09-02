package com.example.mymandalapp.core.utils

object ValidationUtils {

    fun isValidMandalId(id: String): Boolean {
        return id.isNotBlank() && id.length >= 4 && id.all { it.isLetterOrDigit() }
    }

    fun isValidMobile(mobile: String): Boolean {
        if (mobile.isBlank()) return true // Optional
        return mobile.length == 10 && mobile.all { it.isDigit() }
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true // Optional
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidAmount(amount: String): Boolean {
        val value = amount.toDoubleOrNull()
        return value != null && value > 0
    }

    fun isValidQuantity(qty: String): Boolean {
        val value = qty.toDoubleOrNull()
        return value != null && value > 0
    }

    fun validatePassword(password: String): String? {
        if (password.length < 6) return "Password must be at least 6 characters."
        return null
    }

    fun trim(text: String): String = text.trim()

    /**
     * Sanitizes input but allows Indian language characters (Unicode).
     */
    fun isNotEmpty(text: String): Boolean = text.trim().isNotEmpty()
}
