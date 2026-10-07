package com.antigravity.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.expensetracker.data.local.entity.ParsingRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsingRuleDao {

    @Query("SELECT * FROM parsing_rules")
    fun getAllRulesFlow(): Flow<List<ParsingRuleEntity>>

    @Query("SELECT * FROM parsing_rules WHERE package_name = :packageName")
    suspend fun getRulesForPackage(packageName: String): List<ParsingRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ParsingRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ParsingRuleEntity>)

    @Delete
    suspend fun delete(rule: ParsingRuleEntity)
}
