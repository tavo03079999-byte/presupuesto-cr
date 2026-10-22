package com.presupuesto.cr.ui.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptionsimport kotlinx.coroutines.tasks.await

data class ScannedReceiptData(
    val merchant: String?,
    val totalAmount: Double,
    val dateString: String?,
    val rawExtractedText: String
)

class ReceiptScannerHelper(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processReceiptBitmap(bitmap: Bitmap): Result<ScannedReceiptData> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val fullText = result.text

            val lines = fullText.lines()
            var detectedMerchant: String? = null
            var detectedTotal = 0.0
            var detectedDate: String? = null

            // Detectar Comercio en las primeras líneas
            for (line in lines.take(5)) {
                val upper = line.uppercase()
                if (upper.contains("WALMART") || upper.contains("AUTOMERCADO") || upper.contains("PALI") ||
                    upper.contains("MASXMENOS") || upper.contains("SUPER") || upper.contains("CARNICERIA")) {
                    detectedMerchant = line.trim()
                    break
                }
            }

            // Buscar Total (Líneas con TOTAL, MONTO, SUB-TOTAL, PAGADO, etc.)
            val totalRegex = Regex("""(?:TOTAL|MONTO|PAGO|CRC|₡)\s*[:=]?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""", RegexOption.IGNORE_CASE)
            for (line in lines) {
                val match = totalRegex.find(line)
                if (match != null) {
                    val raw = match.groupValues[1].replace(".", "").replace(",", ".")
                    val candidate = raw.toDoubleOrNull() ?: 0.0
                    if (candidate > detectedTotal) {
                        detectedTotal = candidate
                    }
                }
            }

            // Buscar Fecha (dd/MM/yyyy o dd-MM-yyyy)
            val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")
            for (line in lines) {
                val match = dateRegex.find(line)
                if (match != null) {
                    detectedDate = match.groupValues[1]
                    break
                }
            }

            Result.success(
                ScannedReceiptData(
                    merchant = detectedMerchant ?: "Comercio Escaneado",
                    totalAmount = detectedTotal,
                    dateString = detectedDate,
                    rawExtractedText = fullText
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
