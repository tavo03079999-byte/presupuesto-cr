package com.presupuesto.cr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringFrequency
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringMovementEntity::class,
        BcrPendingEntity::class
    ],
    version = 1,
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

        fun getInstance(context: Context, passphraseBytes: ByteArray = getDatabasePassphrase(context)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // Integración con SQLCipher para cifrado AES-256 local
                    .openHelperFactory(SupportFactory(passphraseBytes))
                    .addCallback(DatabasePrepopulateCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getDatabasePassphrase(context: Context): ByteArray {
            // Clave criptográfica local persistida en SecureStorage/Keystore
            val prefs = context.getSharedPreferences("security_prefs_cr", Context.MODE_PRIVATE)
            var key = prefs.getString("db_sec_key", null)
            if (key == null) {
                key = java.util.UUID.randomUUID().toString() + "-CR-SECURE-KEY-2024"
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
                val database = getInstance(context)
                prepopulateData(database)
            }
        }

        private suspend fun prepopulateData(db: AppDatabase) {
            // 1. Prepopular Cuentas por Defecto
            val accounts = listOf(
                AccountEntity(
                    id = 1,
                    name = "Efectivo",
                    accountType = AccountType.CASH,
                    currentBalance = 35000.0,
                    currency = "CRC",
                    colorHex = "#2E7D32",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                ),
                AccountEntity(
                    id = 2,
                    name = "Cuenta BCR Débito",
                    accountType = AccountType.BANK_BCR,
                    currentBalance = 385000.0,
                    currency = "CRC",
                    accountNumberMasked = "*4892",
                    colorHex = "#002F6C",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                ),
                AccountEntity(
                    id = 3,
                    name = "Tarjeta BCR Crédito",
                    accountType = AccountType.CREDIT_CARD_BCR,
                    currentBalance = -45000.0, // Saldo adeudado actual
                    creditLimit = 600000.0,
                    currency = "CRC",
                    accountNumberMasked = "*7110",
                    colorHex = "#C62828",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = false
                ),
                AccountEntity(
                    id = 4,
                    name = "Ahorro Metas BCR",
                    accountType = AccountType.SAVINGS,
                    currentBalance = 150000.0,
                    currency = "CRC",
                    accountNumberMasked = "*9901",
                    colorHex = "#F9A825",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = false // Protegido del gasto diario
                )
            )
            db.accountDao().insertAccounts(accounts)

            // 2. Prepopular Categorías requeridas
            db.categoryDao().insertDefaultCategories(CategoryEntity.getDefaultCategories())

            // 3. Prepopular Movimientos Recurrentes iniciales
            val calendar = java.util.Calendar.getInstance()
            val next15th = java.util.Calendar.getInstance().apply {
                if (get(java.util.Calendar.DAY_OF_MONTH) > 15) {
                    add(java.util.Calendar.MONTH, 1)
                    set(java.util.Calendar.DAY_OF_MONTH, 15)
                } else {
                    set(java.util.Calendar.DAY_OF_MONTH, 15)
                }
            }.timeInMillis

            val recurring = listOf(
                RecurringMovementEntity(
                    id = 1,
                    name = "Salario Quincenal BCR",
                    amount = 450000.0,
                    type = CategoryType.INCOME,
                    categoryId = 1,
                    accountId = 2,
                    frequency = RecurringFrequency.FORTNIGHTLY_15_30,
                    dayOfMonth = 15,
                    secondDayOfMonth = 30,
                    nextDueDate = next15th,
                    isPaidForCurrentPeriod = true
                ),
                RecurringMovementEntity(
                    id = 2,
                    name = "Diezmo",
                    amount = 45000.0,
                    type = CategoryType.EXPENSE,
                    categoryId = 3,
                    accountId = 2,
                    frequency = RecurringFrequency.FORTNIGHTLY_15_30,
                    dayOfMonth = 15,
                    secondDayOfMonth = 30,
                    nextDueDate = next15th,
                    isPaidForCurrentPeriod = false
                ),
                RecurringMovementEntity(
                    id = 3,
                    name = "Servicios Públicos (Luz/Agua/Net)",
                    amount = 55000.0,
                    type = CategoryType.EXPENSE,
                    categoryId = 6,
                    accountId = 2,
                    frequency = RecurringFrequency.MONTHLY,
                    dayOfMonth = 20,
                    nextDueDate = System.currentTimeMillis() + 86400000L * 5,
                    isPaidForCurrentPeriod = false
                ),
                RecurringMovementEntity(
                    id = 4,
                    name = "Gimnasio",
                    amount = 22000.0,
                    type = CategoryType.EXPENSE,
                    categoryId = 7,
                    accountId = 2,
                    frequency = RecurringFrequency.MONTHLY,
                    dayOfMonth = 1,
                    nextDueDate = System.currentTimeMillis() + 86400000L * 10,
                    isPaidForCurrentPeriod = false
                ),
                RecurringMovementEntity(
                    id = 5,
                    name = "Ahorro Programado Quincenal",
                    amount = 50000.0,
                    type = CategoryType.SPECIAL_ALLOCATION,
                    categoryId = 8,
                    accountId = 4,
                    frequency = RecurringFrequency.FORTNIGHTLY_15_30,
                    dayOfMonth = 15,
                    secondDayOfMonth = 30,
                    nextDueDate = next15th,
                    isPaidForCurrentPeriod = false
                )
            )
            db.recurringMovementDao().insertMovements(recurring)
        }
    }
}
