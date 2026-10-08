package com.antigravity.expensetracker.data.repository

import com.antigravity.expensetracker.data.deduplication.DeduplicationResult
import com.antigravity.expensetracker.data.deduplication.TransferDeduplicator
import com.antigravity.expensetracker.data.local.dao.AccountDao
import com.antigravity.expensetracker.data.local.dao.CategoryDao
import com.antigravity.expensetracker.data.local.dao.TransactionDao
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import com.antigravity.expensetracker.data.model.TransactionStatus
import com.antigravity.expensetracker.data.model.TransactionType
import com.antigravity.expensetracker.data.parser.ParsedOutput
import com.antigravity.expensetracker.data.parser.TransactionParserEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val parserEngine: TransactionParserEngine = TransactionParserEngine(),
    private val deduplicator: TransferDeduplicator = TransferDeduplicator(transactionDao)
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()

    fun getTotalExpenses(): Flow<Double> = transactionDao.getTotalExpensesFlow()

    fun getTotalIncome(): Flow<Double> = transactionDao.getTotalIncomeFlow()

    suspend fun processNotification(
        packageName: String,
        title: String,
        content: String,
        postTime: Long
    ): ParsedOutput? {
        val fullContent = "$title $content".trim()
        val parsed = parserEngine.parse(fullContent, postTime) ?: return null

        val accounts = accountDao.getAllAccounts()
        val matchedAccount = if (parsed.accountIdentifier != null) {
            accounts.firstOrNull { it.identifierMask == parsed.accountIdentifier }
                ?: accounts.firstOrNull()
        } else {
            accounts.firstOrNull()
        }

        val accountId = matchedAccount?.id ?: "acc_checking_01"
        val categoryId = matchCategory(parsed.counterparty, parsed.type)

        val newTransaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            destinationAccountId = null,
            categoryId = categoryId,
            amount = parsed.amount,
            currency = parsed.currency,
            type = parsed.type.name,
            counterparty = parsed.counterparty,
            rawText = sanitizeRawText(fullContent),
            timestamp = postTime,
            status = TransactionStatus.CONFIRMED.name
        )

        val dedupResult = deduplicator.evaluate(newTransaction)
        when (dedupResult) {
            is DeduplicationResult.MergedAsTransfer -> {
                // Insert/update merged transfer
                transactionDao.insert(dedupResult.transferTransaction)
                // Mark superseded record as EXCLUDED to avoid duplicate balance calculations
                val superseded = transactionDao.getTransactionById(dedupResult.supersededTransactionId)
                if (superseded != null) {
                    transactionDao.update(superseded.copy(status = TransactionStatus.EXCLUDED.name))
                }
            }
            is DeduplicationResult.Standalone -> {
                transactionDao.insert(dedupResult.transaction)
                // Update account balance
                matchedAccount?.let { acc ->
                    val newBalance = if (parsed.type == TransactionType.DEBIT) {
                        acc.currentBalance - parsed.amount
                    } else {
                        acc.currentBalance + parsed.amount
                    }
                    accountDao.updateBalance(acc.id, newBalance)
                }
            }
        }

        return parsed
    }

    suspend fun insertManualTransaction(
        accountId: String,
        destinationAccountId: String?,
        categoryId: String?,
        amount: Double,
        currency: String,
        type: TransactionType,
        counterparty: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            destinationAccountId = destinationAccountId,
            categoryId = categoryId,
            amount = amount,
            currency = currency,
            type = type.name,
            counterparty = counterparty,
            rawText = "Manual Entry: $counterparty ($amount $currency)",
            timestamp = timestamp,
            status = TransactionStatus.CONFIRMED.name
        )

        val dedupResult = deduplicator.evaluate(transaction)
        when (dedupResult) {
            is DeduplicationResult.MergedAsTransfer -> {
                transactionDao.insert(dedupResult.transferTransaction)
                val superseded = transactionDao.getTransactionById(dedupResult.supersededTransactionId)
                if (superseded != null) {
                    transactionDao.update(superseded.copy(status = TransactionStatus.EXCLUDED.name))
                }
            }
            is DeduplicationResult.Standalone -> {
                transactionDao.insert(dedupResult.transaction)
                val account = accountDao.getAccountById(accountId)
                account?.let { acc ->
                    val newBalance = when (type) {
                        TransactionType.DEBIT -> acc.currentBalance - amount
                        TransactionType.CREDIT -> acc.currentBalance + amount
                        TransactionType.TRANSFER -> acc.currentBalance - amount
                    }
                    accountDao.updateBalance(acc.id, newBalance)
                }
                if (type == TransactionType.TRANSFER && destinationAccountId != null) {
                    val destAccount = accountDao.getAccountById(destinationAccountId)
                    destAccount?.let { dest ->
                        accountDao.updateBalance(dest.id, dest.currentBalance + amount)
                    }
                }
            }
        }
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.deleteById(id)
    }

    private fun matchCategory(counterparty: String, type: TransactionType): String {
        val cp = counterparty.lowercase()
        return when {
            type == TransactionType.CREDIT -> "cat_income_06"
            cp.contains("starbucks") || cp.contains("coffee") || cp.contains("highlands") ||
            cp.contains("dining") || cp.contains("restaurant") || cp.contains("food") -> "cat_food_01"
            cp.contains("uber") || cp.contains("lyft") || cp.contains("grab") ||
            cp.contains("gas") || cp.contains("petrol") || cp.contains("taxi") -> "cat_transport_02"
            cp.contains("amazon") || cp.contains("target") || cp.contains("walmart") ||
            cp.contains("shopee") || cp.contains("lazada") || cp.contains("tiki") -> "cat_shopping_03"
            cp.contains("netflix") || cp.contains("spotify") || cp.contains("cinema") ||
            cp.contains("movie") || cp.contains("game") -> "cat_entertainment_04"
            else -> "cat_bills_05"
        }
    }

    companion object {
        private val CARD_NUMBER_PATTERN = Pattern.compile(
            "\\b(?:\\d[ -]*?){13,19}\\b"
        )

        /**
         * Redacts full credit/debit card numbers from stored raw notifications (OWASP MASVS-STORAGE / PII Protection).
         * Leaves only the last 4 digits visible (e.g. "**** **** **** 1234").
         */
        fun sanitizeRawText(text: String): String {
            if (text.isBlank()) return text
            val matcher = CARD_NUMBER_PATTERN.matcher(text)
            val sb = StringBuffer()
            while (matcher.find()) {
                val fullMatch = matcher.group()
                val digitsOnly = fullMatch.filter { it.isDigit() }
                if (digitsOnly.length in 13..19) {
                    val last4 = digitsOnly.takeLast(4)
                    val masked = "**** **** **** $last4"
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(masked))
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(fullMatch))
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }
    }
}
