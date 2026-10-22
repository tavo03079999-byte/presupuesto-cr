package com.presupuesto.cr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.data.local.entity.TransactionSource
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.ForecastResult
import com.presupuesto.cr.domain.model.SafeToSpendResult
import com.presupuesto.cr.domain.usecase.CalculateSafeToSpendUseCase
import com.presupuesto.cr.domain.usecase.ForecastEngineUseCase
import com.presupuesto.cr.domain.usecase.ParsedVoiceTransaction
import com.presupuesto.cr.domain.usecase.ProcessGeminiVoiceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: FinanceRepository,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val forecastEngineUseCase: ForecastEngineUseCase,
    private val processGeminiVoiceUseCase: ProcessGeminiVoiceUseCase
) : ViewModel() {

    private val _safeToSpend = MutableStateFlow<SafeToSpendResult?>(null)
    val safeToSpend: StateFlow<SafeToSpendResult?> = _safeToSpend.asStateFlow()

    private val _quickForecast = MutableStateFlow<ForecastResult?>(null)
    val quickForecast: StateFlow<ForecastResult?> = _quickForecast.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingBcrCount: StateFlow<Int> = repository.getPendingBcrCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isProcessingVoice = MutableStateFlow(false)
    val isProcessingVoice: StateFlow<Boolean> = _isProcessingVoice.asStateFlow()

    private val _parsedTransactionToConfirm = MutableStateFlow<ParsedVoiceTransaction?>(null)
    val parsedTransactionToConfirm: StateFlow<ParsedVoiceTransaction?> = _parsedTransactionToConfirm.asStateFlow()

    init {
        refreshFinancialState()
    }

    fun refreshFinancialState() {
        viewModelScope.launch {
            _safeToSpend.value = calculateSafeToSpendUseCase()
            _quickForecast.value = forecastEngineUseCase(forecastMonths = 1)
        }
    }

    fun startListening() {
        _isListening.value = true
    }

    fun onSpeechComplete(transcript: String) {
        _isListening.value = false
        if (transcript.isBlank()) return

        _isProcessingVoice.value = true
        viewModelScope.launch {
            val result = processGeminiVoiceUseCase(transcript)
            result.onSuccess { parsed ->
                _parsedTransactionToConfirm.value = parsed
            }
            _isProcessingVoice.value = false
        }
    }

    fun confirmTransaction(parsed: ParsedVoiceTransaction) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    accountId = parsed.accountId,
                    categoryId = parsed.categoryId,
                    amount = parsed.amount,
                    type = parsed.type,
                    timestamp = System.currentTimeMillis(),
                    note = parsed.note,
                    merchant = parsed.merchant,
                    source = TransactionSource.VOICE_GEMINI
                )
            )
            _parsedTransactionToConfirm.value = null
            refreshFinancialState()
        }
    }

    fun dismissConfirmDialog() {
        _parsedTransactionToConfirm.value = null
    }
}
