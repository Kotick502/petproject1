package com.example.expensestracker.domain

enum class TransactionType(val title: String) {
    EXPENSE("Расход"),
    INCOME("Доход")
}

object TransactionCategories {
    val expenses = listOf("Еда", "Транспорт", "Жилье", "Здоровье", "Покупки", "Развлечения", "Другое")
    val incomes = listOf("Зарплата", "Фриланс", "Подарки", "Инвестиции", "Возврат", "Другое")

    fun forType(type: TransactionType): List<String> = when (type) {
        TransactionType.EXPENSE -> expenses
        TransactionType.INCOME -> incomes
    }
}
