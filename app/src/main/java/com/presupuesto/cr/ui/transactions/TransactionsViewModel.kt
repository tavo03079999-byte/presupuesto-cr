package com.presupuesto.cr.ui.transactions

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.BcrPendingEntity
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.data.local.entity.TransactionSource
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.ui.ocr.ReceiptScannerHelper
import com.presupuesto.cr.ui.ocr.ScannedReceiptData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val repository: FinanceRepository,
    private val receiptScannerHelper: ReceiptScannerHelper
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringMovements: StateFlow<List<RecurringMovementEntity>> = repository.getAllRecurringMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingBcrTransactions: StateFlow<List<BcrPendingEntity>> = repository.getPendingBcrTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanningOcr = MutableStateFlow(false)
    val isScanningOcr: StateFlow<Boolean> = _isScanningOcr.asStateFlow()

    private val _scannedReceiptResult = MutableStateFlow<ScannedReceiptData?>(null)
    val scannedReceiptResult: StateFlow<ScannedReceiptData?> = _scannedReceiptResult.asStateFlow()

    fun addManualTransaction(
        accountId: Long,
        categoryId: Long,
        amount: Double,
        type: CategoryType,
        note: String,
        merchant: String? = null,
        source: TransactionSource = TransactionSource.MANUAL
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    note = note,
                    merchant = merchant,
                    source = source
                )
            )
        }
    }

    fun processReceiptImage(bitmap: Bitmap) {
        _isScanningOcr.value = true
        viewModelScope.launch {
            val result = receiptScannerHelper.processReceiptBitmap(bitmap)
            result.onSuccess { data ->
                _scannedReceiptResult.value = data
            }
            _isScanningOcr.value = false
        }
    }

    fun clearScannedReceipt() {
        _scannedReceiptResult.value = null
    }

    fun approveBcrPending(entity: BcrPendingEntity, categoryId: Long, accountId: Long) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = entity.parsedAmount,
                    type = CategoryType.EXPENSE,
                    timestamp = entity.timestamp,
                    note = "Compra BCR: ${entity.merchant ?: "Comercio"}",
                    merchant = entity.merchant,
                    source = TransactionSource.BCR_NOTIFICATION
                )
            )
            repository.markBcrAsProcessed(entity.id)
        }
    }

    fun discardBcrPending(entity: BcrPendingEntity) {
        viewModelScope.launch {
            repository.deleteBcrPending(entity)
        }
    }

    fun toggleRecurringPaid(movement: RecurringMovementEntity) {
        viewModelScope.launch {
            repository.setRecurringPaidStatus(movement.id, !movement.isPaidForCurrentPeriod)
        }
    }
}
