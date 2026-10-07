package com.antigravity.expensetracker.data.deduplication

import com.antigravity.expensetracker.data.local.dao.TransactionDao
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import com.antigravity.expensetracker.data.model.TransactionStatus
import com.antigravity.expensetracker.data.model.TransactionType
import kotlin.math.abs

sealed class DeduplicationResult {
    data class Standalone(val transaction: TransactionEntity) : DeduplicationResult()
    data class MergedAsTransfer(
        val transferTransaction: TransactionEntity,
        val supersededTransactionId: String
    ) : DeduplicationResult()
}

class TransferDeduplicator(
    private val transactionDao: TransactionDao? = null,
    private val timeWindowMillis: Long = 180_000L // 3 minutes buffer
) {

    /**
     * Checks if the incoming transaction pairs with an existing transaction within the 3-minute window.
     */
    suspend fun evaluate(incoming: TransactionEntity): DeduplicationResult {
        if (transactionDao == null) {
            return DeduplicationResult.Standalone(incoming)
        }

        // Only DEBIT and CREDIT can be deduplicated into TRANSFER
        if (incoming.type != TransactionType.DEBIT.name && incoming.type != TransactionType.CREDIT.name) {
            return DeduplicationResult.Standalone(incoming)
        }

        val minTime = incoming.timestamp - timeWindowMillis
        val maxTime = incoming.timestamp + timeWindowMillis

        val candidates = transactionDao.findMatchingCandidateForDeduplication(
            amount = incoming.amount,
            minTimestamp = minTime,
            maxTimestamp = maxTime
        )

        val targetOppositeType = if (incoming.type == TransactionType.DEBIT.name) {
            TransactionType.CREDIT.name
        } else {
            TransactionType.DEBIT.name
        }

        val matchedCandidate = candidates.firstOrNull { candidate ->
            candidate.id != incoming.id &&
            candidate.type == targetOppositeType &&
            abs(candidate.timestamp - incoming.timestamp) <= timeWindowMillis
        }

        if (matchedCandidate != null) {
            return if (incoming.type == TransactionType.DEBIT.name) {
                // Incoming is DEBIT, candidate was CREDIT
                val transfer = incoming.copy(
                    type = TransactionType.TRANSFER.name,
                    destinationAccountId = matchedCandidate.accountId,
                    counterparty = "Self-Transfer",
                    status = TransactionStatus.CONFIRMED.name
                )
                DeduplicationResult.MergedAsTransfer(
                    transferTransaction = transfer,
                    supersededTransactionId = matchedCandidate.id
                )
            } else {
                // Incoming is CREDIT, candidate was DEBIT
                val transfer = matchedCandidate.copy(
                    type = TransactionType.TRANSFER.name,
                    destinationAccountId = incoming.accountId,
                    counterparty = "Self-Transfer",
                    status = TransactionStatus.CONFIRMED.name
                )
                DeduplicationResult.MergedAsTransfer(
                    transferTransaction = transfer,
                    supersededTransactionId = incoming.id
                )
            }
        }

        return DeduplicationResult.Standalone(incoming)
    }

    /**
     * In-memory evaluation helper for unit testing.
     */
    fun evaluateInMemory(
        incoming: TransactionEntity,
        existingList: List<TransactionEntity>
    ): DeduplicationResult {
        if (incoming.type != TransactionType.DEBIT.name && incoming.type != TransactionType.CREDIT.name) {
            return DeduplicationResult.Standalone(incoming)
        }

        val targetOppositeType = if (incoming.type == TransactionType.DEBIT.name) {
            TransactionType.CREDIT.name
        } else {
            TransactionType.DEBIT.name
        }

        val matchedCandidate = existingList.firstOrNull { candidate ->
            candidate.id != incoming.id &&
            candidate.type == targetOppositeType &&
            abs(candidate.amount - incoming.amount) < 0.001 &&
            abs(candidate.timestamp - incoming.timestamp) <= timeWindowMillis &&
            candidate.status != TransactionStatus.EXCLUDED.name
        }

        if (matchedCandidate != null) {
            return if (incoming.type == TransactionType.DEBIT.name) {
                val transfer = incoming.copy(
                    type = TransactionType.TRANSFER.name,
                    destinationAccountId = matchedCandidate.accountId,
                    counterparty = "Self-Transfer",
                    status = TransactionStatus.CONFIRMED.name
                )
                DeduplicationResult.MergedAsTransfer(transfer, matchedCandidate.id)
            } else {
                val transfer = matchedCandidate.copy(
                    type = TransactionType.TRANSFER.name,
                    destinationAccountId = incoming.accountId,
                    counterparty = "Self-Transfer",
                    status = TransactionStatus.CONFIRMED.name
                )
                DeduplicationResult.MergedAsTransfer(transfer, incoming.id)
            }
        }

        return DeduplicationResult.Standalone(incoming)
    }
}
