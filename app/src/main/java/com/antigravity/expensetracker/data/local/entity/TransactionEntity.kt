package com.antigravity.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destination_account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("account_id"),
        Index("destination_account_id"),
        Index("category_id"),
        Index("timestamp")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "destination_account_id")
    val destinationAccountId: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    val amount: Double,
    val currency: String,
    val type: String, // DEBIT, CREDIT, TRANSFER
    val counterparty: String? = null,
    @ColumnInfo(name = "raw_text")
    val rawText: String,
    val timestamp: Long,
    val status: String = "CONFIRMED" // PENDING, CONFIRMED, EXCLUDED
)
