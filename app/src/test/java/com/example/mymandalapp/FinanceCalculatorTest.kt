package com.example.mymandalapp

import com.example.mymandalapp.core.finance.FinanceCalculator
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculatorTest {

    @Test
    fun `test current balance calculation`() {
        val openingBalance = 2500000L // ₹25,000.00
        val transactions = listOf(
            Transaction(type = TransactionType.INCOME, amountPaise = 500000L), // ₹5,000.00
            Transaction(type = TransactionType.INCOME, amountPaise = 250000L), // ₹2,500.00
            Transaction(type = TransactionType.EXPENSE, amountPaise = 400000L), // ₹4,000.00
            Transaction(type = TransactionType.EXPENSE, amountPaise = 150000L)  // ₹1,500.00
        )

        val expectedBalance = 2500000L + 500000L + 250000L - 400000L - 150000L
        val actualBalance = FinanceCalculator.calculateCurrentBalance(openingBalance, transactions)

        assertEquals(expectedBalance, actualBalance)
        assertEquals(2700000L, actualBalance) // ₹27,000.00
    }

    @Test
    fun `test empty transactions`() {
        val openingBalance = 100000L
        val transactions = emptyList<Transaction>()
        val actualBalance = FinanceCalculator.calculateCurrentBalance(openingBalance, transactions)
        assertEquals(openingBalance, actualBalance)
    }

    @Test
    fun `test payment mode breakdown`() {
        val transactions = listOf(
            Transaction(type = TransactionType.INCOME, amountPaise = 500000L, paymentMode = com.example.mymandalapp.core.finance.PaymentMode.CASH),
            Transaction(type = TransactionType.INCOME, amountPaise = 800000L, paymentMode = com.example.mymandalapp.core.finance.PaymentMode.UPI),
            Transaction(type = TransactionType.EXPENSE, amountPaise = 200000L, paymentMode = com.example.mymandalapp.core.finance.PaymentMode.CASH),
            Transaction(type = TransactionType.EXPENSE, amountPaise = 300000L, paymentMode = com.example.mymandalapp.core.finance.PaymentMode.UPI)
        )

        val incomeByMode = FinanceCalculator.calculateIncomeByPaymentMode(transactions)
        val expenseByMode = FinanceCalculator.calculateExpenseByPaymentMode(transactions)

        assertEquals(500000L, incomeByMode[com.example.mymandalapp.core.finance.PaymentMode.CASH])
        assertEquals(800000L, incomeByMode[com.example.mymandalapp.core.finance.PaymentMode.UPI])
        assertEquals(200000L, expenseByMode[com.example.mymandalapp.core.finance.PaymentMode.CASH])
        assertEquals(300000L, expenseByMode[com.example.mymandalapp.core.finance.PaymentMode.UPI])
    }

    @Test
    fun `test object donations do not affect balance`() {
        val openingBalance = 1000000L // ₹10,000
        val transactions = listOf(
            Transaction(type = TransactionType.INCOME, amountPaise = 500000L), // + ₹5,000
            Transaction(
                type = TransactionType.OBJECT_DONATION, 
                amountPaise = 0L, 
                itemName = "Sugar", 
                quantity = "10", 
                unit = "Kg",
                estimatedValuePaise = 50000L // ₹500 estimated but should be ignored
            ),
            Transaction(type = TransactionType.EXPENSE, amountPaise = 300000L) // - ₹3,000
        )

        val actualBalance = FinanceCalculator.calculateCurrentBalance(openingBalance, transactions)
        val expectedBalance = 1000000L + 500000L - 300000L

        assertEquals(expectedBalance, actualBalance)
        assertEquals(1200000L, actualBalance) // ₹12,000
        assertEquals(1, FinanceCalculator.calculateObjectDonationCount(transactions))
    }
}
