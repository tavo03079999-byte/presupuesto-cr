package com.presupuesto.cr.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.CategoryExpenseSummary
import com.presupuesto.cr.domain.model.ForecastResult
import com.presupuesto.cr.domain.usecase.ExportPdfReportUseCase
import com.presupuesto.cr.domain.usecase.ForecastEngineUseCase
import com.presupuesto.cr.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class TimeFilter(val label: String) {
    WEEK("Semana"),
    MONTH("Mes"),
    YEAR("Año"),
    CUSTOM("Personalizado")
}

enum class PeriodComparisonMode(val label: String) {
    MONTH_VS_PREV("Mes vs. Anterior"),
    WEEK_VS_PREV("Semana vs. Anterior"),
    TWO_CUSTOM_RANGES("2 Intervalos")
}

data class ComparisonStats(
    val periodALabel: String = "",
    val periodAIncome: Double = 0.0,
    val periodAExpense: Double = 0.0,
    val periodBLabel: String = "",
    val periodBIncome: Double = 0.0,
    val periodBExpense: Double = 0.0
)

data class ExportedShareFile(
    val file: File,
    val mimeType: String
)

class ReportsViewModel(
    private val repository: FinanceRepository,
    private val forecastEngineUseCase: ForecastEngineUseCase,
    private val exportPdfReportUseCase: ExportPdfReportUseCase
) : ViewModel() {

    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.MONTH)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()

    private val initialMonthBounds = DateUtils.getCurrentMonthBounds()
    private val _customStartDate = MutableStateFlow(initialMonthBounds.first)
    val customStartDate: StateFlow<Long> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(initialMonthBounds.second)
    val customEndDate: StateFlow<Long> = _customEndDate.asStateFlow()

    private val _comparisonMode = MutableStateFlow(PeriodComparisonMode.MONTH_VS_PREV)
    val comparisonMode: StateFlow<PeriodComparisonMode> = _comparisonMode.asStateFlow()

    private val prevMonthBounds = DateUtils.getPreviousMonthBounds()
    private val _compRangeAStart = MutableStateFlow(prevMonthBounds.first)
    val compRangeAStart: StateFlow<Long> = _compRangeAStart.asStateFlow()
    private val _compRangeAEnd = MutableStateFlow(prevMonthBounds.second)
    val compRangeAEnd: StateFlow<Long> = _compRangeAEnd.asStateFlow()

    private val _compRangeBStart = MutableStateFlow(initialMonthBounds.first)
    val compRangeBStart: StateFlow<Long> = _compRangeBStart.asStateFlow()
    private val _compRangeBEnd = MutableStateFlow(initialMonthBounds.second)
    val compRangeBEnd: StateFlow<Long> = _compRangeBEnd.asStateFlow()

    private val _comparisonStats = MutableStateFlow(ComparisonStats())
    val comparisonStats: StateFlow<ComparisonStats> = _comparisonStats.asStateFlow()

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

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportedShareFile = MutableStateFlow<ExportedShareFile?>(null)
    val exportedShareFile: StateFlow<ExportedShareFile?> = _exportedShareFile.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            repository.getAllTransactions().collect {
                loadData()
            }
        }
    }

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
        loadData()
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val safeStart = DateUtils.getStartOfDay(minOf(startMillis, endMillis))
        val safeEnd = DateUtils.getEndOfDay(maxOf(startMillis, endMillis))
        _customStartDate.value = safeStart
        _customEndDate.value = safeEnd
        _selectedTimeFilter.value = TimeFilter.CUSTOM
        loadData()
    }

    fun setComparisonMode(mode: PeriodComparisonMode) {
        _comparisonMode.value = mode
        loadComparison()
    }

    fun setCustomComparisonRangeA(startMillis: Long, endMillis: Long) {
        _compRangeAStart.value = DateUtils.getStartOfDay(minOf(startMillis, endMillis))
        _compRangeAEnd.value = DateUtils.getEndOfDay(maxOf(startMillis, endMillis))
        loadComparison()
    }

    fun setCustomComparisonRangeB(startMillis: Long, endMillis: Long) {
        _compRangeBStart.value = DateUtils.getStartOfDay(minOf(startMillis, endMillis))
        _compRangeBEnd.value = DateUtils.getEndOfDay(maxOf(startMillis, endMillis))
        loadComparison()
    }

    fun setForecastHorizon(months: Int) {
        _forecastMonths.value = months
        viewModelScope.launch {
            _forecastResult.value = forecastEngineUseCase(months)
        }
    }

    fun getActiveRangeBounds(): Pair<Long, Long> {
        return getTimeBounds(_selectedTimeFilter.value)
    }

    fun loadData() {
        viewModelScope.launch {
            val (startTime, endTime) = getTimeBounds(_selectedTimeFilter.value)

            _forecastResult.value = forecastEngineUseCase(_forecastMonths.value)
            _totalIncome.value = repository.getTotalByTypeBetweenSync(CategoryType.INCOME, startTime, endTime)
            _totalExpense.value = repository.getTotalByTypeBetweenSync(CategoryType.EXPENSE, startTime, endTime)
            _totalSavings.value = repository.getTotalByTypeBetweenSync(CategoryType.SPECIAL_ALLOCATION, startTime, endTime)

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

            loadComparison()
        }
    }

    private fun loadComparison() {
        viewModelScope.launch {
            val (aStart, aEnd, aLabel) = when (_comparisonMode.value) {
                PeriodComparisonMode.MONTH_VS_PREV -> {
                    val b = DateUtils.getPreviousMonthBounds()
                    Triple(b.first, b.second, "Mes anterior")
                }
                PeriodComparisonMode.WEEK_VS_PREV -> {
                    val b = DateUtils.getPreviousWeekBounds()
                    Triple(b.first, b.second, "Semana anterior")
                }
                PeriodComparisonMode.TWO_CUSTOM_RANGES -> {
                    Triple(
                        _compRangeAStart.value,
                        _compRangeAEnd.value,
                        "${DateUtils.formatShort(_compRangeAStart.value)} - ${DateUtils.formatShort(_compRangeAEnd.value)}"
                    )
                }
            }

            val (bStart, bEnd, bLabel) = when (_comparisonMode.value) {
                PeriodComparisonMode.MONTH_VS_PREV -> {
                    val b = DateUtils.getCurrentMonthBounds()
                    Triple(b.first, b.second, "Este mes")
                }
                PeriodComparisonMode.WEEK_VS_PREV -> {
                    val b = DateUtils.getCurrentWeekBounds()
                    Triple(b.first, b.second, "Esta semana")
                }
                PeriodComparisonMode.TWO_CUSTOM_RANGES -> {
                    Triple(
                        _compRangeBStart.value,
                        _compRangeBEnd.value,
                        "${DateUtils.formatShort(_compRangeBStart.value)} - ${DateUtils.formatShort(_compRangeBEnd.value)}"
                    )
                }
            }

            val incA = repository.getTotalByTypeBetweenSync(CategoryType.INCOME, aStart, aEnd)
            val expA = repository.getTotalByTypeBetweenSync(CategoryType.EXPENSE, aStart, aEnd)
            val incB = repository.getTotalByTypeBetweenSync(CategoryType.INCOME, bStart, bEnd)
            val expB = repository.getTotalByTypeBetweenSync(CategoryType.EXPENSE, bStart, bEnd)

            _comparisonStats.value = ComparisonStats(
                periodALabel = aLabel,
                periodAIncome = incA,
                periodAExpense = expA,
                periodBLabel = bLabel,
                periodBIncome = incB,
                periodBExpense = expB
            )
        }
    }

    fun exportPdfReport() {
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val (start, end) = getTimeBounds(_selectedTimeFilter.value)
                val file = exportPdfReportUseCase.exportPdf(start, end)
                _exportedShareFile.value = ExportedShareFile(file, "application/pdf")
            } catch (e: Exception) {
                // Manejo seguro al generar PDF
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportCsvReport() {
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val (start, end) = getTimeBounds(_selectedTimeFilter.value)
                val file = exportPdfReportUseCase.exportCsv(start, end)
                _exportedShareFile.value = ExportedShareFile(file, "text/csv")
            } catch (e: Exception) {
                // Manejo seguro al generar CSV
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportedFile() {
        _exportedShareFile.value = null
    }

    private fun getTimeBounds(filter: TimeFilter): Pair<Long, Long> {
        return when (filter) {
            TimeFilter.WEEK -> DateUtils.getCurrentWeekBounds()
            TimeFilter.MONTH -> DateUtils.getCurrentMonthBounds()
            TimeFilter.YEAR -> {
                val cal = Calendar.getInstance()
                val end = DateUtils.getEndOfDay(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                DateUtils.getStartOfDay(cal.timeInMillis) to end
            }
            TimeFilter.CUSTOM -> _customStartDate.value to _customEndDate.value
        }
    }
}
