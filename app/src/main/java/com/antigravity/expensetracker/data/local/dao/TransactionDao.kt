package com.antigravity.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    fun getTransactionsByStatusFlow(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentTransactions(sinceTimestamp: Long): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount 
          AND timestamp BETWEEN :minTimestamp AND :maxTimestamp
          AND status != 'EXCLUDED'
        ORDER BY timestamp DESC
    """)
    suspend fun findMatchingCandidateForDeduplication(
        amount: Double,
        minTimestamp: Long,
        maxTimestamp: Long
    ): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEBIT' AND status = 'CONFIRMED'")
    fun getTotalExpensesFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'CREDIT' AND status = 'CONFIRMED'")
    fun getTotalIncomeFlow(): Flow<Double>
}
