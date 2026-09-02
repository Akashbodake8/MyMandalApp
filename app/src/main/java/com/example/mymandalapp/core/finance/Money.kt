package com.example.mymandalapp.core.finance

import java.text.NumberFormat
import java.util.Locale

/**
 * Utility for handling financial calculations in Paise (Long) to avoid floating-point errors.
 * 1 Rupee = 100 Paise.
 */
object Money {

    private val indianLocale = Locale("en", "IN")
    private val currencyFormat = NumberFormat.getCurrencyInstance(indianLocale).apply {
        val symbols = (this as java.text.DecimalFormat).decimalFormatSymbols
        symbols.currencySymbol = "₹"
        this.decimalFormatSymbols = symbols
    }

    fun fromRupees(amount: Double): Long {
        return (amount * 100).toLong()
    }

    fun fromRupees(amount: String): Long {
        val cleaned = amount.replace("[^0-9.]".toRegex(), "")
        return if (cleaned.isEmpty()) 0L else fromRupees(cleaned.toDouble())
    }

    fun toRupees(paise: Long): Double {
        return paise / 100.0
    }

    fun format(paise: Long): String {
        return currencyFormat.format(toRupees(paise))
    }

    /**
     * Converts paise to words in Indian English (e.g., Rupees Five Thousand One Only).
     */
    fun toWords(paise: Long): String {
        val rupees = paise / 100
        if (rupees == 0L) return "Zero Rupees Only"
        
        return "Rupees ${convertToWords(rupees)} Only"
    }

    private fun convertToWords(n: Long): String {
        val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

        if (n < 20) return units[n.toInt()]
        if (n < 100) return tens[(n / 10).toInt()] + (if (n % 10 != 0L) " " + units[(n % 10).toInt()] else "")
        if (n < 1000) return units[(n / 100).toInt()] + " Hundred" + (if (n % 100 != 0L) " " + convertToWords(n % 100) else "")
        if (n < 100000) return convertToWords(n / 1000) + " Thousand" + (if (n % 1000 != 0L) " " + convertToWords(n % 1000) else "")
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh" + (if (n % 100000 != 0L) " " + convertToWords(n % 100000) else "")
        return convertToWords(n / 10000000) + " Crore" + (if (n % 10000000 != 0L) " " + convertToWords(n % 10000000) else "")
    }
}
