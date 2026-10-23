package com.presupuesto.cr.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class ExportPdfReportUseCase(
    private val context: Context,
    private val repository: FinanceRepository,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase
) {
    suspend operator fun invoke(
        startTime: Long? = null,
        endTime: Long? = null
    ): File = exportPdf(startTime, endTime)

    suspend fun exportCsv(
        startTime: Long? = null,
        endTime: Long? = null
    ): File = withContext(Dispatchers.IO) {
        val categoriesMap = repository.getAllCategories().first().associateBy { it.id }
        val transactions = if (startTime != null && endTime != null) {
            repository.getTransactionsBetweenSync(startTime, endTime)
        } else {
            repository.getAllTransactions().first()
        }

        val reportsDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(reportsDir, "Transacciones_PresupuestoCR_${System.currentTimeMillis()}.csv")

        FileOutputStream(file).use { fos ->
            BufferedWriter(OutputStreamWriter(fos, Charsets.UTF_8)).use { writer ->
                writer.write("\uFEFF")
                writer.write("Fecha,Descripcion,Categoria,Tipo,Monto_CRC")
                writer.newLine()

                for (tx in transactions) {
                    val fecha = DateUtils.formatDate(tx.timestamp)
                    val descripcion = tx.note.replace("\"", "\"\"")
                    val categoria = (categoriesMap[tx.categoryId]?.name ?: "General").replace("\"", "\"\"")
                    val tipo = when (tx.type) {
                        CategoryType.INCOME -> "Ingreso"
                        CategoryType.EXPENSE -> "Gasto"
                        CategoryType.SPECIAL_ALLOCATION -> "Ahorro"
                    }
                    val monto = String.format(java.util.Locale.US, "%.2f", tx.amount)
                    writer.write("\"$fecha\",\"$descripcion\",\"$categoria\",\"$tipo\",$monto")
                    writer.newLine()
                }
            }
        }
        file
    }

    suspend fun exportPdf(
        startTime: Long? = null,
        endTime: Long? = null
    ): File = withContext(Dispatchers.IO) {
        val safeToSpend = calculateSafeToSpendUseCase()
        val categoriesMap = repository.getAllCategories().first().associateBy { it.id }
        val transactions = if (startTime != null && endTime != null) {
            repository.getTransactionsBetweenSync(startTime, endTime)
        } else {
            repository.getAllTransactions().first()
        }

        val totalIncome = transactions.filter { it.type == CategoryType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == CategoryType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        val now = System.currentTimeMillis()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D162C")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#555555")
            textSize = 10f
        }

        val headerBoxPaint = Paint().apply {
            color = Color.parseColor("#E3F2FD")
        }

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9.5f
        }

        val boldTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            strokeWidth = 1f
        }

        var y = 40f

        canvas.drawText("PRESUPUESTO CR — REPORTE FINANCIERO", 40f, y, titlePaint)
        y += 16f
        val rangeLabel = if (startTime != null && endTime != null) {
            "Periodo: ${DateUtils.formatDate(startTime)} al ${DateUtils.formatDate(endTime)}"
        } else {
            "Periodo: Historial general"
        }
        canvas.drawText("$rangeLabel  |  Emitido: ${DateUtils.formatFriendly(now)}", 40f, y, subtitlePaint)
        y += 18f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        canvas.drawRoundRect(40f, y, 555f, y + 68f, 8f, 8f, headerBoxPaint)
        canvas.drawText("RESUMEN DEL PERIODO SELECCIONADO", 55f, y + 20f, boldTextPaint)
        canvas.drawText("Ingresos: ${CurrencyFormatter.formatCrc(totalIncome)}", 55f, y + 40f, textPaint)
        canvas.drawText("Gastos: ${CurrencyFormatter.formatCrc(totalExpense)}", 225f, y + 40f, textPaint)
        canvas.drawText("Balance Neto: ${CurrencyFormatter.formatCrc(netBalance)}", 385f, y + 40f, boldTextPaint)
        canvas.drawText("Dinero Libre Diario Hoy: ${CurrencyFormatter.formatCrc(safeToSpend.safeToSpendDailyToday)}", 55f, y + 58f, subtitlePaint)
        y += 90f

        canvas.drawText("DETALLE DE TRANSACCIONES", 40f, y, sectionPaint)
        y += 18f
        canvas.drawText("Fecha", 42f, y, boldTextPaint)
        canvas.drawText("Descripción", 105f, y, boldTextPaint)
        canvas.drawText("Categoría", 285f, y, boldTextPaint)
        canvas.drawText("Tipo", 405f, y, boldTextPaint)
        canvas.drawText("Monto", 475f, y, boldTextPaint)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 14f

        for (tx in transactions.take(36)) {
            if (y > 805f) break
            val catName = categoriesMap[tx.categoryId]?.name ?: "General"
            val noteTrunc = if (tx.note.length > 28) tx.note.substring(0, 26) + ".." else tx.note.ifBlank { catName }
            val catTrunc = if (catName.length > 18) catName.substring(0, 16) + ".." else catName
            val typeStr = when (tx.type) {
                CategoryType.INCOME -> "Ingreso"
                CategoryType.EXPENSE -> "Gasto"
                CategoryType.SPECIAL_ALLOCATION -> "Ahorro"
            }

            canvas.drawText(DateUtils.formatDate(tx.timestamp), 42f, y, textPaint)
            canvas.drawText(noteTrunc, 105f, y, textPaint)
            canvas.drawText(catTrunc, 285f, y, textPaint)
            canvas.drawText(typeStr, 405f, y, textPaint)
            canvas.drawText(CurrencyFormatter.formatCrc(tx.amount), 475f, y, textPaint)
            y += 14f
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
        }
        canvas.drawText("Generado por PresupuestoCR — tavo03079999@gmail.com", 40f, 825f, footerPaint)

        pdfDocument.finishPage(page)

        val reportsDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(reportsDir, "Reporte_PresupuestoCR_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        file
    }
}
