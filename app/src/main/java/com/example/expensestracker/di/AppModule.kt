package com.example.expensestracker.di

import android.content.Context
import androidx.room.Room
import com.example.expensestracker.data.local.AppDatabase
import com.example.expensestracker.data.local.TransactionDao
import com.example.expensestracker.data.repository.RoomTransactionRepository
import com.example.expensestracker.data.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expenses_tracker.db"
        ).build()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        repository: RoomTransactionRepository
    ): TransactionRepository
}
