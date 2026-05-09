package com.example.expensestracker.data.repository

import com.example.expensestracker.data.local.TransactionDao
import com.example.expensestracker.data.local.toDomain
import com.example.expensestracker.data.local.toEntity
import com.example.expensestracker.domain.MoneyTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {
    override fun observeAll(): Flow<List<MoneyTransaction>> =
        dao.observeAll().map { transactions -> transactions.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<MoneyTransaction?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(transaction: MoneyTransaction): Long =
        dao.upsert(transaction.toEntity())

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
