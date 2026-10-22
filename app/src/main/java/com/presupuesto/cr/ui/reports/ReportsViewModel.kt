package com.presupuesto.cr.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.CategoryExpenseSummary
import com.presupuesto.cr.domain.model.ForecastResult
import com.presupuesto.cr.domain.usecase.ExportPdfReportUseCase
import com.presupuesto.cr.domain.usecase.ForecastEngineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class TimeFilter(val label: String) {
    DAY("Día"),
    WEEK("Semana"),
    FORTNIGHT("Quincena"),
    MONTH("Mes"),
    YEAR("Año")
}

class ReportsViewModel(
    private val repository: FinanceRepository,
    private val forecastEngineUseCase: ForecastEngineUseCase,
    private val exportPdfReportUseCase: ExportPdfReportUseCase
) : ViewModel() {

    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.MONTH)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()

    private val _forecastMonths = MutableStateFlow(3)
    val forecastMonths: StateFlow<Int> = _forecastMonths.asStateFlow()

    private val _forecastResult = MutableStateFlow<ForecastResult?>(null)
    val forecastResult: StateFlow<ForecastResult?> = _forecastResult.asStateFlow()

    private val _categoriesSummary = MutableStateFlow<List<CategoryExpenseSummary>>(emptyList())
    val categoriesSummary: StateFlow<List<CategoryExpenseSummary>> = _categoriesSummary.asStateFlow()

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome.asStateFlow()

    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense.asStateFlow()

    private val _totalSavings = MutableStateFlow(0.0)
    val totalSavings: StateFlow<Double> = _totalSavings.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _exportedPdfFile = MutableStateFlow<File?>(null)
    val exportedPdfFile: StateFlow<File?> = _exportedPdfFile.asStateFlow()

    init {
        loadData()
    }

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
        loadData()
    }

    fun setForecastHorizon(months: Int) {
        _forecastMonths.value = months
        viewModelScope.launch {
            _forecastResult.value = forecastEngineUseCase(months)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            val (startTime, endTime) = getTimeBounds(_selectedTimeFilter.value)

            // Cargar Proyección Predictiva
            _forecastResult.value = forecastEngineUseCase(_forecastMonths.value)

            // Cargar Comparativa de Totales
            _totalIncome.value = repository.getTotalByTypeBetweenSync(CategoryType.INCOME, startTime, endTime)
            _totalExpense.value = repository.getTotalByTypeBetweenSync(CategoryType.EXPENSE, startTime, endTime)
            _totalSavings.value = repository.getTotalByTypeBetweenSync(CategoryType.SPECIAL_ALLOCATION, startTime, endTime)

            // Cargar desglose de categorías
            val expenseSummary = repository.getExpenseSummaryByCategory(startTime, endTime).first()
            val totalSpent = expenseSummary.sumOf { it.totalAmount }

            _categoriesSummary.value = expenseSummary.map {
                val percentage = if (totalSpent > 0) (it.totalAmount / totalSpent) * 100.0 else 0.0
                CategoryExpenseSummary(
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    colorHex = it.colorHex,
                    spentAmount = it.totalAmount,
                    percentageOfTotal = percentage
                )
            }
        }
    }

    fun exportPdfReport() {
        _isExportingPdf.value = true
        viewModelScope.launch {
            try {
                val file = exportPdfReportUseCase()
                _exportedPdfFile.value = file
            } catch (e: Exception) {
                // error
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    fun clearExportedPdf() {
        _exportedPdfFile.value = null
    }

    private fun getTimeBounds(filter: TimeFilter): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        when (filter) {
            TimeFilter.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            TimeFilter.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            TimeFilter.FORTNIGHT -> {
                calendar.add(Calendar.DAY_OF_YEAR, -15)
            }
            TimeFilter.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
            TimeFilter.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
        }
        return calendar.timeInMillis to endTime
    }
}
