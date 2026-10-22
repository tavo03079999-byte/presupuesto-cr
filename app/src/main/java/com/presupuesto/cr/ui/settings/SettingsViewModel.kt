package com.presupuesto.cr.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.presupuesto.cr.data.local.dao.BudgetWithCategory
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val repository: FinanceRepository,
    private val googleDriveBackupUseCase: GoogleDriveBackupUseCase
) : ViewModel() {

    private val prefs = context.getSharedPreferences("settings_prefs_cr", Context.MODE_PRIVATE)

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetsWithCategories: StateFlow<List<BudgetWithCategory>> = repository.getBudgetsWithCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun performGoogleDriveBackup(account: GoogleSignInAccount?) {
        if (account == null) {
            _backupStatus.value = "Inicia sesión con tavo03079999@gmail.com para respaldar"
            return
        }

        _isBackingUp.value = true
        _backupStatus.value = "Subiendo respaldo cifrado a Google Drive..."
        viewModelScope.launch {
            val result = googleDriveBackupUseCase.uploadDatabaseBackup(account)
            result.onSuccess { msg ->
                _backupStatus.value = msg
            }.onFailure { err ->
                _backupStatus.value = "Error al respaldar: ${err.localizedMessage}"
            }
            _isBackingUp.value = false
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}
