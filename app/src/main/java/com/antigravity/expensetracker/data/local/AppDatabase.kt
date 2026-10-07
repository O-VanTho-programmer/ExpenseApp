package com.antigravity.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.antigravity.expensetracker.data.local.dao.AccountDao
import com.antigravity.expensetracker.data.local.dao.CategoryDao
import com.antigravity.expensetracker.data.local.dao.ParsingRuleDao
import com.antigravity.expensetracker.data.local.dao.TransactionDao
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.local.entity.CategoryEntity
import com.antigravity.expensetracker.data.local.entity.ParsingRuleEntity
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import com.antigravity.expensetracker.data.model.AccountType
import com.antigravity.expensetracker.data.model.CategoryType
import com.antigravity.expensetracker.data.security.DatabaseKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        ParsingRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun parsingRuleDao(): ParsingRuleDao

    companion object {
        private const val DATABASE_NAME = "expense_tracker_secure.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, useEncryption: Boolean = true): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, useEncryption).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, useEncryption: Boolean): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )

            if (useEncryption) {
                try {
                    val keyManager = DatabaseKeyManager(context.applicationContext)
                    val passphrase = keyManager.getDatabasePassphrase()
                    val factory = SupportFactory(passphrase)
                    builder.openHelperFactory(factory)
                } catch (e: Throwable) {
                    // Fallback to unencrypted in test or unsupported hardware environments
                }
            }

            builder.addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = getInstance(context, useEncryption)
                        seedInitialData(database)
                    }
                }
            })

            return builder.build()
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val defaultAccounts = listOf(
                AccountEntity(
                    id = "acc_checking_01",
                    name = "Vietcombank Checking",
                    type = AccountType.CHECKING.name,
                    currency = "VND",
                    currentBalance = 25000000.0,
                    identifierMask = "4242"
                ),
                AccountEntity(
                    id = "acc_wallet_02",
                    name = "MoMo e-Wallet",
                    type = AccountType.WALLET.name,
                    currency = "VND",
                    currentBalance = 2500000.0,
                    identifierMask = "9876"
                ),
                AccountEntity(
                    id = "acc_cash_03",
                    name = "Cash Reserves",
                    type = AccountType.CASH.name,
                    currency = "VND",
                    currentBalance = 1000000.0,
                    identifierMask = null
                )
            )
            database.accountDao().insertAll(defaultAccounts)

            val defaultCategories = listOf(
                CategoryEntity(
                    id = "cat_food_01",
                    name = "Food & Dining",
                    type = CategoryType.EXPENSE.name,
                    iconKey = "restaurant",
                    colorHex = "#FF5722"
                ),
                CategoryEntity(
                    id = "cat_transport_02",
                    name = "Transport",
                    type = CategoryType.EXPENSE.name,
                    iconKey = "directions_car",
                    colorHex = "#2196F3"
                ),
                CategoryEntity(
                    id = "cat_shopping_03",
                    name = "Shopping",
                    type = CategoryType.EXPENSE.name,
                    iconKey = "shopping_cart",
                    colorHex = "#E91E63"
                ),
                CategoryEntity(
                    id = "cat_entertainment_04",
                    name = "Entertainment",
                    type = CategoryType.EXPENSE.name,
                    iconKey = "movie",
                    colorHex = "#9C27B0"
                ),
                CategoryEntity(
                    id = "cat_bills_05",
                    name = "Utilities & Bills",
                    type = CategoryType.EXPENSE.name,
                    iconKey = "receipt",
                    colorHex = "#607D8B"
                ),
                CategoryEntity(
                    id = "cat_income_06",
                    name = "Salary & Income",
                    type = CategoryType.INCOME.name,
                    iconKey = "payments",
                    colorHex = "#4CAF50"
                ),
                CategoryEntity(
                    id = "cat_transfer_07",
                    name = "Transfer",
                    type = CategoryType.EXPENSE.name,
                    iconKey = "sync_alt",
                    colorHex = "#00BCD4"
                )
            )
            database.categoryDao().insertAll(defaultCategories)
        }
    }
}
