package com.example.expensestracker.data.repository

import com.example.expensestracker.domain.MoneyTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<MoneyTransaction>>
    fun observeById(id: Long): Flow<MoneyTransaction?>
    suspend fun upsert(transaction: MoneyTransaction): Long
    suspend fun deleteById(id: Long)
}
