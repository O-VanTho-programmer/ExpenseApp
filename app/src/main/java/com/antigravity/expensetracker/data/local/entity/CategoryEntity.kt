package com.antigravity.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // EXPENSE, INCOME
    @ColumnInfo(name = "icon_key")
    val iconKey: String? = null,
    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null
)
