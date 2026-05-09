package com.example.expensestracker.domain

data class MoneyTransaction(
    val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val category: String,
    val type: TransactionType,
    val dateMillis: Long,
    val note: String = ""
)

data class CategoryTotal(
    val category: String,
    val type: TransactionType,
    val amountCents: Long
)

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val incomeCents: Long,
    val expenseCents: Long,
    val categoryTotals: List<CategoryTotal>
) {
    val balanceCents: Long = incomeCents - expenseCents
}
