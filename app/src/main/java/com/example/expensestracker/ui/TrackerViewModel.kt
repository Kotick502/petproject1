package com.example.expensestracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensestracker.data.repository.TransactionRepository
import com.example.expensestracker.domain.CategoryTotal
import com.example.expensestracker.domain.MoneyTransaction
import com.example.expensestracker.domain.MonthlyStats
import com.example.expensestracker.domain.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackerUiState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val expenses: List<MoneyTransaction> = emptyList(),
    val incomes: List<MoneyTransaction> = emptyList(),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val monthlyStats: List<MonthlyStats> = emptyList()
) {
    val balanceCents: Long = incomeCents - expenseCents
}

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    val uiState: StateFlow<TrackerUiState> = repository.observeAll()
        .map { transactions -> transactions.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrackerUiState()
        )

    fun save(transaction: MoneyTransaction) {
        viewModelScope.launch {
            repository.upsert(transaction)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun List<MoneyTransaction>.toUiState(): TrackerUiState {
        val expenses = filter { it.type == TransactionType.EXPENSE }
        val incomes = filter { it.type == TransactionType.INCOME }
        return TrackerUiState(
            transactions = this,
            expenses = expenses,
            incomes = incomes,
            incomeCents = incomes.sumOf { it.amountCents },
            expenseCents = expenses.sumOf { it.amountCents },
            monthlyStats = buildMonthlyStats(this)
        )
    }

    private fun buildMonthlyStats(transactions: List<MoneyTransaction>): List<MonthlyStats> {
        val zone = ZoneId.systemDefault()
        return transactions
            .groupBy { transaction ->
                YearMonth.from(
                    Instant.ofEpochMilli(transaction.dateMillis).atZone(zone)
                )
            }
            .map { (month, monthTransactions) ->
                val incomeCents = monthTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountCents }
                val expenseCents = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountCents }
                val categoryTotals = monthTransactions
                    .groupBy { it.type to it.category }
                    .map { (key, grouped) ->
                        CategoryTotal(
                            category = key.second,
                            type = key.first,
                            amountCents = grouped.sumOf { it.amountCents }
                        )
                    }
                    .sortedWith(
                        compareBy<CategoryTotal> { it.type.ordinal }
                            .thenByDescending { it.amountCents }
                    )

                MonthlyStats(
                    year = month.year,
                    month = month.monthValue,
                    incomeCents = incomeCents,
                    expenseCents = expenseCents,
                    categoryTotals = categoryTotals
                )
            }
            .sortedWith(compareByDescending<MonthlyStats> { it.year }.thenByDescending { it.month })
    }
}
