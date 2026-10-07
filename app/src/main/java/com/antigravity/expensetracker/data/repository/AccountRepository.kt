package com.antigravity.expensetracker.data.repository

import com.antigravity.expensetracker.data.local.dao.AccountDao
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccountsFlow()

    suspend fun getAccountById(id: String): AccountEntity? = accountDao.getAccountById(id)

    suspend fun addAccount(account: AccountEntity) {
        accountDao.insert(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.update(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.delete(account)
    }

    suspend fun updateBalance(id: String, newBalance: Double) {
        accountDao.updateBalance(id, newBalance)
    }
}
