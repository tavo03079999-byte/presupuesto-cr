package com.presupuesto.cr.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.SafeToSpendResult
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ExportPdfReportUseCase(
    private val context: Context,
    private val repository: FinanceRepository,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase
) {
    suspend operator fun invoke(): File = withContext(Dispatchers.IO) {
        val safeToSpend = calculateSafeToSpendUseCase()
        val accounts = repository.getAllAccounts().first()
        val transactions = repository.getAllTransactions().first().take(20)
        val now = System.currentTimeMillis()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Formato A4 estándar en puntos (72 dpi)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#002F6C") // Azul BCR
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#555555")
            textSize = 10f
        }

        val headerBoxPaint = Paint().apply {
            color = Color.parseColor("#E8F5E9") // Verde claro esmeralda
        }

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#00875A") // Verde Esmeralda
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 10f
        }

        val boldTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            strokeWidth = 1f
        }

        var y = 40f

        // 1. Encabezado
        canvas.drawText("PRESUPUESTO INTELIGENTE COSTA RICA", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("Reporte Ejecutivo de Flujo de Caja y Dinero Libre (Safe-to-Spend)", 40f, y, subtitlePaint)
        y += 14f
        canvas.drawText("Usuario: tavo03079999@gmail.com  |  Fecha de emisión: ${DateUtils.formatFriendly(now)}", 40f, y, subtitlePaint)
        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // 2. Caja Destacada: Dinero Libre (Safe-to-Spend)
        canvas.drawRoundRect(40f, y, 555f, y + 80f, 8f, 8f, headerBoxPaint)
        canvas.drawText("DINERO LIBRE DISPONIBLE HOY:", 55f, y + 25f, boldTextPaint)

        val amountPaint = Paint().apply {
            color = Color.parseColor("#00875A")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(CurrencyFormatter.formatCrc(safeToSpend.safeToSpendDailyToday), 55f, y + 55f, amountPaint)
        canvas.drawText("Diario (₡${CurrencyFormatter.formatCrc(safeToSpend.totalSafeToSpendFortnight)} restante en quincena)", 260f, y + 52f, textPaint)
        canvas.drawText(safeToSpend.healthMessage, 55f, y + 72f, subtitlePaint)
        y += 105f

        // 3. Resumen de Cuentas y Carteras
        canvas.drawText("ESTADO DE CUENTAS CONSOLIDADAS", 40f, y, sectionPaint)
        y += 15f
        for (account in accounts) {
            canvas.drawText(account.name, 45f, y, textPaint)
            val balanceStr = CurrencyFormatter.formatCrc(account.currentBalance)
            canvas.drawText(balanceStr, 450f, y, boldTextPaint)
            y += 15f
        }
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // 4. Compromisos Fijos y Metas Pendientes del Periodo
        canvas.drawText("COMPROMISOS PENDIENTES DEL PERIODO", 40f, y, sectionPaint)
        y += 16f
        canvas.drawText("Gastos Fijos Pendientes (Diezmo, Servicios, etc.):", 45f, y, textPaint)
        canvas.drawText(CurrencyFormatter.formatCrc(safeToSpend.pendingFixedExpenses), 450f, y, textPaint)
        y += 15f
        canvas.drawText("Metas Programadas de Ahorro / Inversión:", 45f, y, textPaint)
        canvas.drawText(CurrencyFormatter.formatCrc(safeToSpend.pendingScheduledSavings), 450f, y, textPaint)
        y += 15f
        canvas.drawText("Saldo Adeudado Tarjetas BCR:", 45f, y, textPaint)
        canvas.drawText(CurrencyFormatter.formatCrc(safeToSpend.upcomingCreditCardDebt), 450f, y, textPaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // 5. Historial Reciente de Transacciones
        canvas.drawText("ÚLTIMAS TRANSACCIONES REGISTRADAS", 40f, y, sectionPaint)
        y += 16f
        canvas.drawText("Fecha", 45f, y, boldTextPaint)
        canvas.drawText("Descripción / Nota", 120f, y, boldTextPaint)
        canvas.drawText("Tipo", 380f, y, boldTextPaint)
        canvas.drawText("Monto (CRC)", 470f, y, boldTextPaint)
        y += 12f

        for (tx in transactions) {
            if (y > 800f) break
            canvas.drawText(DateUtils.formatShort(tx.timestamp), 45f, y, textPaint)
            val noteTruncated = if (tx.note.length > 32) tx.note.substring(0, 30) + "..." else tx.note
            canvas.drawText(noteTruncated, 120f, y, textPaint)
            canvas.drawText(tx.type.name, 380f, y, textPaint)
            canvas.drawText(CurrencyFormatter.formatCrc(tx.amount), 470f, y, textPaint)
            y += 14f
        }

        // Pie de página
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
        }
        canvas.drawText("Generado automáticamente por Presupuesto Inteligente CR - Confidencial para tavo03079999@gmail.com", 40f, 825f, footerPaint)

        pdfDocument.finishPage(page)

        // Guardar archivo
        val reportsDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(reportsDir, "Reporte_Financiero_CR_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        file
    }
}
