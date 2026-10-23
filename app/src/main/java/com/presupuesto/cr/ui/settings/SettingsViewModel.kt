package com.presupuesto.cr.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.presupuesto.cr.data.local.dao.BudgetWithCategory
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.BudgetAlertLevel
import com.presupuesto.cr.domain.model.CategoryBudgetProgress
import com.presupuesto.cr.domain.usecase.ExportPdfReportUseCase
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import com.presupuesto.cr.util.DateUtils
import com.presupuesto.cr.worker.PaymentReminderWorker
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExportedSettingsFile(
    val file: File,
    val mimeType: String,
    val chooserTitle: String
)

class SettingsViewModel(
    private val context: Context,
    private val repository: FinanceRepository,
    private val googleDriveBackupUseCase: GoogleDriveBackupUseCase,
    private val exportPdfReportUseCase: ExportPdfReportUseCase
) : ViewModel() {

    private val prefs = context.getSharedPreferences("settings_prefs_cr", Context.MODE_PRIVATE)

    private val _isDarkThemeEnabled = MutableStateFlow(prefs.getBoolean("dark_mode_enabled", true))
    val isDarkThemeEnabled: StateFlow<Boolean> = _isDarkThemeEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportedFile = MutableStateFlow<ExportedSettingsFile?>(null)
    val exportedFile: StateFlow<ExportedSettingsFile?> = _exportedFile.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetsWithCategories: StateFlow<List<BudgetWithCategory>> = repository.getBudgetsWithCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetProgressList: StateFlow<List<CategoryBudgetProgress>> = combine(
        budgetsWithCategories,
        allTransactions
    ) { budgets, txs ->
        val (monthStart, monthEnd) = DateUtils.getCurrentMonthBounds()
        budgets.map { b ->
            val spent = txs.filter {
                it.type == CategoryType.EXPENSE &&
                    it.categoryId == b.categoryId &&
                    it.timestamp in monthStart..monthEnd
            }.sumOf { it.amount }
            val pct = if (b.limitAmount > 0.0) (spent / b.limitAmount) * 100.0 else 0.0
            val alertLevel = when {
                pct >= 100.0 -> BudgetAlertLevel.EXCEEDED_100
                pct >= 80.0 -> BudgetAlertLevel.WARNING_80
                else -> BudgetAlertLevel.NORMAL
            }
            CategoryBudgetProgress(
                budgetId = b.id,
                categoryId = b.categoryId,
                categoryName = b.categoryName,
                categoryColorHex = b.categoryColor,
                iconName = b.iconName,
                spentAmount = spent,
                limitAmount = b.limitAmount,
                remainingAmount = b.limitAmount - spent,
                percentageUsed = pct,
                alertLevel = alertLevel
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDarkTheme(enabled: Boolean) {
        _isDarkThemeEnabled.value = enabled
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun saveCategory(
        existing: CategoryEntity?,
        name: String,
        type: CategoryType,
        iconName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            try {
                val trimmed = name.trim()
                if (trimmed.isBlank()) return@launch

                if (existing == null) {
                    val newCat = CategoryEntity(
                        name = trimmed,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex,
                        isFixedExpense = false,
                        isSpecialBalanceAllocation = type == CategoryType.SPECIAL_ALLOCATION
                    )
                    repository.insertCategory(newCat)
                    _uiMessage.value = "Categoría \"$trimmed\" creada exitosamente."
                } else {
                    val updated = existing.copy(
                        name = trimmed,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateCategory(updated)
                    _uiMessage.value = "Categoría \"$trimmed\" actualizada."
                }
            } catch (e: Exception) {
                _uiMessage.value = "No se pudo guardar la categoría."
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                _uiMessage.value = "Categoría \"${category.name}\" eliminada."
            } catch (e: Exception) {
                _uiMessage.value = "No fue posible eliminar la categoría."
            }
        }
    }

    fun saveCategoryBudget(categoryId: Long, limitAmount: Double) {
        viewModelScope.launch {
            try {
                if (limitAmount <= 0.0) return@launch
                repository.saveMonthlyBudgetForCategory(categoryId, limitAmount)
                val (monthStart, monthEnd) = DateUtils.getCurrentMonthBounds()
                val spent = repository.getCategorySpentBetweenSync(categoryId, monthStart, monthEnd)
                val categoryName = repository.getCategoryById(categoryId)?.name ?: "Categoría"
                if (spent >= limitAmount) {
                    PaymentReminderWorker.triggerBudgetExceededNotification(
                        context = context,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        spentAmount = spent,
                        limitAmount = limitAmount
                    )
                }
                _uiMessage.value = "Presupuesto mensual guardado para \"$categoryName\"."
            } catch (e: Exception) {
                _uiMessage.value = "No se pudo guardar el presupuesto mensual."
            }
        }
    }

    fun deleteCategoryBudget(budgetId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteBudgetById(budgetId)
                _uiMessage.value = "Presupuesto mensual eliminado."
            } catch (e: Exception) {
                _uiMessage.value = "No se pudo eliminar el presupuesto."
            }
        }
    }

    fun exportTransactions(
        asCsv: Boolean,
        startMillis: Long?,
        endMillis: Long?
    ) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launch {
            try {
                if (asCsv) {
                    val file = exportPdfReportUseCase.exportCsv(startMillis, endMillis)
                    _exportedFile.value = ExportedSettingsFile(
                        file = file,
                        mimeType = "text/csv",
                        chooserTitle = "Compartir transacciones en CSV"
                    )
                } else {
                    val file = exportPdfReportUseCase.exportPdf(startMillis, endMillis)
                    _exportedFile.value = ExportedSettingsFile(
                        file = file,
                        mimeType = "application/pdf",
                        chooserTitle = "Compartir reporte en PDF"
                    )
                }
            } catch (e: Exception) {
                _uiMessage.value = "No fue posible exportar el archivo en este momento."
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportedFile() {
        _exportedFile.value = null
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun performGoogleDriveBackup(account: GoogleSignInAccount?) {
        if (account == null) {
            _backupStatus.value = "Inicia sesión con tavo03079999@gmail.com para respaldar"
            return
        }

        _isBackingUp.value = true
        _backupStatus.value = "Subiendo respaldo cifrado a Google Drive..."
        viewModelScope.launch {
            try {
                val result = googleDriveBackupUseCase.uploadDatabaseBackup(account)
                result.onSuccess { msg ->
                    _backupStatus.value = msg
                }.onFailure { err ->
                    _backupStatus.value = err.message ?: "No fue posible completar el respaldo en Google Drive."
                }
            } catch (e: Exception) {
                _backupStatus.value = "No fue posible conectar con Google Drive."
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}
