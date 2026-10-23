package com.presupuesto.cr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.presupuesto.cr.BuildConfig
import com.presupuesto.cr.data.local.dao.AccountDao
import com.presupuesto.cr.data.local.dao.BcrPendingDao
import com.presupuesto.cr.data.local.dao.BudgetDao
import com.presupuesto.cr.data.local.dao.CategoryDao
import com.presupuesto.cr.data.local.dao.RecurringMovementDao
import com.presupuesto.cr.data.local.dao.TransactionDao
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.AccountType
import com.presupuesto.cr.data.local.entity.BcrPendingEntity
import com.presupuesto.cr.data.local.entity.BudgetEntity
import com.presupuesto.cr.data.local.entity.BudgetPeriod
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringFrequency
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.data.local.entity.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory
import java.util.Calendar
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringMovementEntity::class,
        BcrPendingEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringMovementDao(): RecurringMovementDao
    abstract fun bcrPendingDao(): BcrPendingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "presupuesto_cr_encrypted.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("ALTER TABLE accounts ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE accounts ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE categories ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE categories ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE transactions ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE transactions ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE budgets ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE budgets ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE bcr_pending_transactions ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE bcr_pending_transactions ADD COLUMN updated_at INTEGER NOT NULL DEFAULT $now")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payment_reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `type` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `dayOfMonth` INTEGER NOT NULL,
                        `secondDayOfMonth` INTEGER,
                        `nextDueDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `isPaidForCurrentPeriod` INTEGER NOT NULL,
                        `autoDeductOnDueDate` INTEGER NOT NULL,
                        `reminderOffsetHours` INTEGER NOT NULL DEFAULT 24,
                        `created_at` INTEGER NOT NULL DEFAULT $now,
                        `updated_at` INTEGER NOT NULL DEFAULT $now
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context, passphraseBytes: ByteArray = getDatabasePassphrase(context)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(SupportFactory(passphraseBytes))
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabasePrepopulateCallback(context))

                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private fun getDatabasePassphrase(context: Context): ByteArray {
            val prefs = context.getSharedPreferences("security_prefs_cr", Context.MODE_PRIVATE)
            var key = prefs.getString("db_sec_key", null)
            if (key == null) {
                key = UUID.randomUUID().toString() + "-CR-SECURE-KEY-2024"
                prefs.edit().putString("db_sec_key", key).apply()
            }
            return key.toByteArray(Charsets.UTF_8)
        }
    }

    private class DatabasePrepopulateCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = getInstance(context)
                    prepopulateData(database)
                } catch (e: Exception) {
                    // Prevención segura en pre-población inicial
                }
            }
        }

        private suspend fun prepopulateData(db: AppDatabase) {
            val accounts = listOf(
                AccountEntity(
                    id = 1,
                    name = "Efectivo",
                    accountType = AccountType.CASH,
                    currentBalance = 65000.0,
                    currency = "CRC",
                    colorHex = "#4CAF50",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                ),
                AccountEntity(
                    id = 2,
                    name = "Cuenta BCR Débito",
                    accountType = AccountType.BANK_BCR,
                    currentBalance = 580000.0,
                    currency = "CRC",
                    accountNumberMasked = "*4892",
                    colorHex = "#2196F3",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                ),
                AccountEntity(
                    id = 3,
                    name = "Tarjeta BCR Crédito",
                    accountType = AccountType.CREDIT_CARD_BCR,
                    currentBalance = -42000.0,
                    creditLimit = 600000.0,
                    currency = "CRC",
                    accountNumberMasked = "*7110",
                    colorHex = "#F44336",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = false
                ),
                AccountEntity(
                    id = 4,
                    name = "Ahorro Metas BCR",
                    accountType = AccountType.SAVINGS,
                    currentBalance = 240000.0,
                    currency = "CRC",
                    accountNumberMasked = "*9901",
                    colorHex = "#FFC107",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = false
                )
            )
            db.accountDao().insertAccounts(accounts)
            db.categoryDao().insertDefaultCategories(CategoryEntity.getDefaultCategories())

            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val sampleTransactions = listOf(
                TransactionEntity(
                    id = 1,
                    accountId = 2,
                    categoryId = 1,
                    amount = 650000.0,
                    type = CategoryType.INCOME,
                    timestamp = now - (dayMs * 6),
                    note = "Salario Quincenal BCR",
                    merchant = "Planilla",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    id = 2,
                    accountId = 2,
                    categoryId = 8,
                    amount = 145000.0,
                    type = CategoryType.EXPENSE,
                    timestamp = now - (dayMs * 5),
                    note = "Cuota Vivienda / Alquiler",
                    merchant = "Transferencia BCR",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    id = 3,
                    accountId = 2,
                    categoryId = 3,
                    amount = 54500.0,
                    type = CategoryType.EXPENSE,
                    timestamp = now - (dayMs * 3),
                    note = "Supermercado Automercado",
                    merchant = "Automercado",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    id = 4,
                    accountId = 1,
                    categoryId = 4,
                    amount = 24000.0,
                    type = CategoryType.EXPENSE,
                    timestamp = now - (dayMs * 2),
                    note = "Gasolina Delta",
                    merchant = "Estación Delta",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    id = 5,
                    accountId = 3,
                    categoryId = 6,
                    amount = 18500.0,
                    type = CategoryType.EXPENSE,
                    timestamp = now - dayMs,
                    note = "Suscripciones y Cine",
                    merchant = "Entretenimiento",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    id = 6,
                    accountId = 2,
                    categoryId = 7,
                    amount = 38000.0,
                    type = CategoryType.EXPENSE,
                    timestamp = now,
                    note = "Recibo Electricidad e Internet",
                    merchant = "CNFL / Kölbi",
                    source = TransactionSource.MANUAL
                )
            )
            db.transactionDao().insertTransactions(sampleTransactions)

            val defaultBudgets = listOf(
                BudgetEntity(id = 1, categoryId = 3, period = BudgetPeriod.MONTHLY, limitAmount = 120000.0, alertThresholdPercent = 80.0),
                BudgetEntity(id = 2, categoryId = 4, period = BudgetPeriod.MONTHLY, limitAmount = 60000.0, alertThresholdPercent = 80.0),
                BudgetEntity(id = 3, categoryId = 6, period = BudgetPeriod.MONTHLY, limitAmount = 22000.0, alertThresholdPercent = 80.0),
                BudgetEntity(id = 4, categoryId = 7, period = BudgetPeriod.MONTHLY, limitAmount = 55000.0, alertThresholdPercent = 80.0)
            )
            db.budgetDao().insertBudgets(defaultBudgets)

            val next15th = Calendar.getInstance().apply {
                if (get(Calendar.DAY_OF_MONTH) > 15) {
                    add(Calendar.MONTH, 1)
                }
                set(Calendar.DAY_OF_MONTH, 15)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            }.timeInMillis

            val recurring = listOf(
                RecurringMovementEntity(
                    id = 1,
                    name = "Alquiler / Vivienda",
                    amount = 145000.0,
                    type = CategoryType.EXPENSE,
                    categoryId = 8,
                    accountId = 2,
                    frequency = RecurringFrequency.MONTHLY,
                    dayOfMonth = 15,
                    secondDayOfMonth = null,
                    nextDueDate = next15th,
                    isPaidForCurrentPeriod = false,
                    reminderOffsetHours = 24
                ),
                RecurringMovementEntity(
                    id = 2,
                    name = "Servicios Públicos (Luz y Agua)",
                    amount = 38000.0,
                    type = CategoryType.EXPENSE,
                    categoryId = 7,
                    accountId = 2,
                    frequency = RecurringFrequency.MONTHLY,
                    dayOfMonth = 20,
                    secondDayOfMonth = null,
                    nextDueDate = now + (dayMs * 4),
                    isPaidForCurrentPeriod = false,
                    reminderOffsetHours = 24
                ),
                RecurringMovementEntity(
                    id = 3,
                    name = "Internet Fibra Óptica",
                    amount = 26500.0,
                    type = CategoryType.EXPENSE,
                    categoryId = 7,
                    accountId = 2,
                    frequency = RecurringFrequency.MONTHLY,
                    dayOfMonth = 25,
                    secondDayOfMonth = null,
                    nextDueDate = now + (dayMs * 7),
                    isPaidForCurrentPeriod = false,
                    reminderOffsetHours = 1
                )
            )
            db.recurringMovementDao().insertMovements(recurring)
        }
    }
}
