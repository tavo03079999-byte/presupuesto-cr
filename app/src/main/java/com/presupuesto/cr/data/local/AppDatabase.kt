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
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import java.util.UUID
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
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                // Limpiar máscaras de demostración pre-cargadas anteriormente
                db.execSQL("UPDATE accounts SET accountNumberMasked = '', isIncludedInSafeToSpend = 1 WHERE accountNumberMasked IN ('*4892', '*7110', '*9901')")
                db.execSQL("UPDATE accounts SET isIncludedInSafeToSpend = 0 WHERE accountType = 'CREDIT_CARD_BCR'")

                // Eliminar transacciones de ejemplo si aún existieran sin haber sido tocadas por el usuario
                db.execSQL("""
                    DELETE FROM transactions
                    WHERE (note = 'Salario Quincenal BCR' AND amount = 650000.0 AND merchant = 'Planilla')
                       OR (note = 'Cuota Vivienda / Alquiler' AND amount = 145000.0 AND merchant = 'Transferencia BCR')
                       OR (note = 'Supermercado Automercado' AND amount = 54500.0 AND merchant = 'Automercado')
                       OR (note = 'Gasolina Delta' AND amount = 24000.0 AND merchant = 'Estación Delta')
                       OR (note = 'Suscripciones y Cine' AND amount = 18500.0 AND merchant = 'Entretenimiento')
                       OR (note = 'Recibo Electricidad e Internet' AND amount = 38000.0 AND merchant = 'CNFL / Kölbi')
                """.trimIndent())

                // Eliminar recordatorios y presupuestos de demostración que el usuario no creó
                db.execSQL("""
                    DELETE FROM payment_reminders
                    WHERE (name = 'Alquiler / Vivienda' AND amount = 145000.0)
                       OR (name = 'Servicios Públicos (Luz y Agua)' AND amount = 38000.0)
                       OR (name = 'Internet Fibra Óptica' AND amount = 26500.0)
                """.trimIndent())

                // Recalcular el saldo real de cada cuenta únicamente con los ingresos y gastos reales del usuario
                db.execSQL("""
                    UPDATE accounts
                    SET currentBalance = COALESCE(
                        (SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                         FROM transactions t
                         WHERE t.accountId = accounts.id),
                        0.0
                    ),
                    updated_at = $now
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
            // Todas las cuentas inician en ₡0 sin saldos ni números ficticios
            val accounts = listOf(
                AccountEntity(
                    id = 1,
                    name = "Efectivo",
                    accountType = AccountType.CASH,
                    currentBalance = 0.0,
                    currency = "CRC",
                    accountNumberMasked = "",
                    colorHex = "#4CAF50",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                ),
                AccountEntity(
                    id = 2,
                    name = "Cuenta BCR Débito",
                    accountType = AccountType.BANK_BCR,
                    currentBalance = 0.0,
                    currency = "CRC",
                    accountNumberMasked = "",
                    colorHex = "#2196F3",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                ),
                AccountEntity(
                    id = 3,
                    name = "Tarjeta BCR Crédito",
                    accountType = AccountType.CREDIT_CARD_BCR,
                    currentBalance = 0.0,
                    creditLimit = 0.0,
                    currency = "CRC",
                    accountNumberMasked = "",
                    colorHex = "#F44336",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = false
                ),
                AccountEntity(
                    id = 4,
                    name = "Cuenta de Ahorros",
                    accountType = AccountType.SAVINGS,
                    currentBalance = 0.0,
                    currency = "CRC",
                    accountNumberMasked = "",
                    colorHex = "#FFC107",
                    isIncludedInNetWorth = true,
                    isIncludedInSafeToSpend = true
                )
            )
            db.accountDao().insertAccounts(accounts)
            db.categoryDao().insertDefaultCategories(CategoryEntity.getDefaultCategories())
        }
    }
}
