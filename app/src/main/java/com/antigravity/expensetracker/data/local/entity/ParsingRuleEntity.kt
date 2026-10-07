package com.antigravity.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "parsing_rules")
data class ParsingRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "regex_pattern")
    val regexPattern: String,
    @ColumnInfo(name = "amount_group_index")
    val amountGroupIndex: Int,
    @ColumnInfo(name = "type_group_index")
    val typeGroupIndex: Int,
    @ColumnInfo(name = "counterparty_group_index")
    val counterpartyGroupIndex: Int? = null,
    @ColumnInfo(name = "account_group_index")
    val accountGroupIndex: Int? = null
)
