package com.example.mymandalapp.core.finance

/**
 * Pure logic for financial calculations.
 */
object FinanceCalculator {

    fun calculateTotalIncome(transactions: List<Transaction>): Long {
        return transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountPaise }
    }

    fun calculateTotalExpenses(transactions: List<Transaction>): Long {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountPaise }
    }

    fun calculateObjectDonationCount(transactions: List<Transaction>): Int {
        return transactions.count { it.type == TransactionType.OBJECT_DONATION }
    }

    fun calculateCurrentBalance(
        openingBalance: Long,
        transactions: List<Transaction>
    ): Long {
        val totalIncome = calculateTotalIncome(transactions)
        val totalExpenses = calculateTotalExpenses(transactions)
        return openingBalance + totalIncome - totalExpenses
    }
    
    fun calculateIncomeByPaymentMode(transactions: List<Transaction>): Map<PaymentMode, Long> {
        return PaymentMode.entries.associateWith { mode ->
            transactions
                .filter { it.type == TransactionType.INCOME && it.paymentMode == mode }
                .sumOf { it.amountPaise }
        }
    }

    fun calculateExpenseByPaymentMode(transactions: List<Transaction>): Map<PaymentMode, Long> {
        return PaymentMode.entries.associateWith { mode ->
            transactions
                .filter { it.type == TransactionType.EXPENSE && it.paymentMode == mode }
                .sumOf { it.amountPaise }
        }
    }

    fun calculatePaymentModeTotals(transactions: List<Transaction>): Map<PaymentMode, Long> {
        val incomeMap = calculateIncomeByPaymentMode(transactions)
        val expenseMap = calculateExpenseByPaymentMode(transactions)
        return PaymentMode.entries.associateWith { mode ->
            (incomeMap[mode] ?: 0L) - (expenseMap[mode] ?: 0L)
        }
    }
}
