package com.antigravity.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // CHECKING, SAVINGS, CREDIT_CARD, WALLET, CASH
    val currency: String = "VND",
    @ColumnInfo(name = "current_balance")
    val currentBalance: Double = 0.0,
    @ColumnInfo(name = "identifier_mask")
    val identifierMask: String? = null,
    @ColumnInfo(name = "saving_sub_type")
    val savingSubType: String? = null // STOCKS, MUTUAL_FUNDS, BONDS, TERM_DEPOSIT, OTHER
)
