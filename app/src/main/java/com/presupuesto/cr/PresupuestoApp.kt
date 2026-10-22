package com.presupuesto.cr

import android.app.Application
import com.presupuesto.cr.data.local.AppDatabase
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.data.repository.FinanceRepositoryImpl
import net.sqlcipher.database.SQLiteDatabase

class PresupuestoApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // Inicializar bibliotecas nativas de cifrado SQLCipher
        SQLiteDatabase.loadLibs(this)

        database = AppDatabase.getInstance(this)
        repository = FinanceRepositoryImpl(database)
    }
}
