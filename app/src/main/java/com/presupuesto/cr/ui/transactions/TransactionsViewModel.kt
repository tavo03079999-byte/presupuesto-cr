package com.presupuesto.cr.ui.transactions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import com.presupuesto.cr.util.DateUtils
import com.presupuesto.cr.worker.PaymentReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionFilterState(
    val searchQuery: String = "",
    val selectedType: CategoryType? = null,
    val selectedCategoryId: Long? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedType != null ||
            selectedCategoryId != null ||
            startDateMillis != null ||
            endDateMillis != null ||
            minAmount != null ||
            maxAmount != null
}

class TransactionsViewModel(
    private val context: Context,
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

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        categories,
        _filterState
    ) { txs, cats, filter ->
        val catMap = cats.associateBy { it.id }
        txs.filter { tx ->
            val catName = catMap[tx.categoryId]?.name ?: ""
            val matchesQuery = if (filter.searchQuery.isBlank()) {
                true
            } else {
                val q = filter.searchQuery.trim().lowercase()
                tx.note.lowercase().contains(q) ||
                    (tx.merchant?.lowercase()?.contains(q) == true) ||
                    catName.lowercase().contains(q)
            }
            val matchesType = filter.selectedType == null || tx.type == filter.selectedType
            val matchesCat = filter.selectedCategoryId == null || tx.categoryId == filter.selectedCategoryId
            val matchesStart = filter.startDateMillis == null || tx.timestamp >= DateUtils.getStartOfDay(filter.startDateMillis)
            val matchesEnd = filter.endDateMillis == null || tx.timestamp <= DateUtils.getEndOfDay(filter.endDateMillis)
            val matchesMin = filter.minAmount == null || tx.amount >= filter.minAmount
            val matchesMax = filter.maxAmount == null || tx.amount <= filter.maxAmount

            matchesQuery && matchesType && matchesCat && matchesStart && matchesEnd && matchesMin && matchesMax
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanningOcr = MutableStateFlow(false)
    val isScanningOcr: StateFlow<Boolean> = _isScanningOcr.asStateFlow()

    private val _scannedReceiptResult = MutableStateFlow<ScannedReceiptData?>(null)
    val scannedReceiptResult: StateFlow<ScannedReceiptData?> = _scannedReceiptResult.asStateFlow()

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateTypeFilter(type: CategoryType?) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun updateCategoryFilter(categoryId: Long?) {
        _filterState.value = _filterState.value.copy(selectedCategoryId = categoryId)
    }

    fun updateDateRangeFilter(startMillis: Long?, endMillis: Long?) {
        _filterState.value = _filterState.value.copy(
            startDateMillis = startMillis,
            endDateMillis = endMillis
        )
    }

    fun updateAmountRangeFilter(minAmount: Double?, maxAmount: Double?) {
        _filterState.value = _filterState.value.copy(
            minAmount = minAmount,
            maxAmount = maxAmount
        )
    }

    fun applyExternalFilter(
        type: CategoryType?,
        categoryId: Long?,
        startMillis: Long? = null,
        endMillis: Long? = null
    ) {
        _filterState.value = TransactionFilterState(
            selectedType = type,
            selectedCategoryId = categoryId,
            startDateMillis = startMillis,
            endDateMillis = endMillis
        )
    }

    fun clearAllFilters() {
        _filterState.value = TransactionFilterState()
    }

    fun addManualTransaction(
        accountId: Long,
        categoryId: Long,
        amount: Double,
        type: CategoryType,
        timestamp: Long,
        note: String,
        merchant: String? = null,
        receiptUris: List<String> = emptyList(),
        source: TransactionSource = TransactionSource.MANUAL
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    timestamp = timestamp,
                    note = note,
                    merchant = merchant,
                    receiptImagePath = encodeImageUris(receiptUris),
                    source = source
                )
            )
            if (type == CategoryType.EXPENSE) {
                checkBudgetThresholdAndNotify(categoryId)
            }
        }
    }

    fun updateExistingTransaction(
        oldTransaction: TransactionEntity,
        accountId: Long,
        categoryId: Long,
        amount: Double,
        type: CategoryType,
        timestamp: Long,
        note: String,
        merchant: String? = null,
        receiptUris: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val updated = oldTransaction.copy(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                type = type,
                timestamp = timestamp,
                note = note,
                merchant = merchant,
                receiptImagePath = encodeImageUris(receiptUris),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTransaction(oldTransaction, updated)
            if (type == CategoryType.EXPENSE) {
                checkBudgetThresholdAndNotify(categoryId)
            }
        }
    }

    fun removeSingleImageFromTransaction(transaction: TransactionEntity, uriToRemove: String) {
        viewModelScope.launch {
            val remaining = parseImageUris(transaction.receiptImagePath).filter { it != uriToRemove }
            val updated = transaction.copy(
                receiptImagePath = encodeImageUris(remaining),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTransaction(transaction, updated)
        }
    }

    private suspend fun checkBudgetThresholdAndNotify(categoryId: Long) {
        try {
            val budget = repository.getBudgetByCategory(categoryId) ?: return
            if (budget.limitAmount <= 0) return
            val (monthStart, monthEnd) = DateUtils.getCurrentMonthBounds()
            val spent = repository.getCategorySpentBetweenSync(categoryId, monthStart, monthEnd)
            if (spent > budget.limitAmount) {
                val categoryName = repository.getCategoryById(categoryId)?.name ?: "Categoría"
                PaymentReminderWorker.triggerBudgetExceededNotification(
                    context = context,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    spentAmount = spent,
                    limitAmount = budget.limitAmount
                )
            }
        } catch (e: Exception) {
            // Manejo seguro de verificación de presupuesto
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun processReceiptImage(bitmap: Bitmap) {
        _isScanningOcr.value = true
        viewModelScope.launch {
            try {
                val result = receiptScannerHelper.processReceiptBitmap(bitmap)
                result.onSuccess { data ->
                    _scannedReceiptResult.value = data
                }
            } catch (e: Exception) {
                // Manejo seguro de OCR
            } finally {
                _isScanningOcr.value = false
            }
        }
    }

    fun processReceiptUri(uri: Uri) {
        _isScanningOcr.value = true
        viewModelScope.launch {
            try {
                val result = receiptScannerHelper.processReceiptUri(uri)
                result.onSuccess { data ->
                    _scannedReceiptResult.value = data
                }
            } catch (e: Exception) {
                // Manejo seguro de OCR
            } finally {
                _isScanningOcr.value = false
            }
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
            checkBudgetThresholdAndNotify(categoryId)
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

    companion object {
        fun parseImageUris(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split("|").map { it.trim() }.filter { it.isNotBlank() }
        }

        fun encodeImageUris(uris: List<String>): String? {
            val clean = uris.map { it.trim() }.filter { it.isNotBlank() }
            return if (clean.isEmpty()) null else clean.joinToString("|")
        }
    }
}
