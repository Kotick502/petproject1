package com.example.expensestracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.expensestracker.domain.MoneyTransaction
import com.example.expensestracker.domain.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val category: String,
    val type: TransactionType,
    @ColumnInfo(name = "date_millis")
    val dateMillis: Long,
    val note: String
)

class TransactionTypeConverter {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}

fun TransactionEntity.toDomain(): MoneyTransaction = MoneyTransaction(
    id = id,
    title = title,
    amountCents = amountCents,
    category = category,
    type = type,
    dateMillis = dateMillis,
    note = note
)

fun MoneyTransaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title,
    amountCents = amountCents,
    category = category,
    type = type,
    dateMillis = dateMillis,
    note = note
)
