package com.presupuesto.cr.domain.usecase

import com.presupuesto.cr.data.local.entity.BcrPendingEntity
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first

class ParseBcrNotificationUseCase(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(notificationTitle: String, notificationBody: String): BcrPendingEntity? {
        val fullText = "$notificationTitle $notificationBody".trim()

        val isBcr = fullText.contains("BCR", ignoreCase = true) ||
            fullText.contains("Banco de Costa Rica", ignoreCase = true) ||
            fullText.contains("SINPE", ignoreCase = true)

        if (!isBcr) return null

        var parsedAmount = 0.0
        val amountRegex = Regex("""(?:CRC|₡|monto(?:\s+de)?|por)\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(fullText)
        if (amountMatch != null) {
            val rawNum = amountMatch.groupValues[1]
                .replace(".", "")
                .replace(",", ".")
            parsedAmount = rawNum.toDoubleOrNull() ?: 0.0
        }

        if (parsedAmount == 0.0) {
            val generalNumberRegex = Regex("""\b([0-9]{1,3}(?:[.,][0-9]{3})+)\b""")
            val generalMatch = generalNumberRegex.find(fullText)
            if (generalMatch != null) {
                val rawNum = generalMatch.groupValues[1].replace(".", "").replace(",", "")
                parsedAmount = rawNum.toDoubleOrNull() ?: 0.0
            }
        }

        if (parsedAmount <= 0.0) return null

        var merchant: String? = null
        val merchantRegex = Regex("""(?:en|comercio|en el comercio|a)\s+([A-Za-z0-9\s\.\-]{3,30}?)(?:\s+(?:el|con|por|tarjeta|fecha)|$)""", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(fullText)
        if (merchantMatch != null) {
            merchant = merchantMatch.groupValues[1].trim()
        }

        var cardLast4: String? = null
        val cardRegex = Regex("""(?:tarjeta|cta|cuenta|\*)\s*(\d{4})""", RegexOption.IGNORE_CASE)
        val cardMatch = cardRegex.find(fullText)
        if (cardMatch != null) {
            cardLast4 = "*${cardMatch.groupValues[1]}"
        }

        val categories = repository.getAllCategories().first()
        val suggestedCategoryId = matchCategoryFromMerchant(merchant ?: fullText, categories)?.id

        return BcrPendingEntity(
            rawText = fullText,
            parsedAmount = parsedAmount,
            merchant = merchant ?: "Comercio BCR",
            cardLast4 = cardLast4 ?: "*BCR",
            timestamp = System.currentTimeMillis(),
            suggestedCategoryId = suggestedCategoryId,
            isProcessed = false
        )
    }

    private fun matchCategoryFromMerchant(text: String, categories: List<CategoryEntity>): CategoryEntity? {
        val lower = text.lowercase()
        return when {
            lower.contains("super") || lower.contains("súper") || lower.contains("walmart") ||
                lower.contains("automercado") || lower.contains("pali") || lower.contains("palí") ||
                lower.contains("masxmenos") || lower.contains("restaurante") || lower.contains("comida") ->
                categories.find { it.name.contains("Aliment", ignoreCase = true) || it.name.contains("Comida", ignoreCase = true) }
            lower.contains("uber") || lower.contains("didi") || lower.contains("gasolin") || lower.contains("delta") || lower.contains("uno") ->
                categories.find { it.name.contains("Transporte", ignoreCase = true) }
            lower.contains("farmacia") || lower.contains("hospital") || lower.contains("clinica") || lower.contains("clínica") ->
                categories.find { it.name.contains("Salud", ignoreCase = true) }
            lower.contains("ice") || lower.contains("kolbi") || lower.contains("kölbi") || lower.contains("cnfl") ||
                lower.contains("aya") || lower.contains("esph") || lower.contains("telecable") || lower.contains("liberty") ->
                categories.find { it.name.contains("Servicios", ignoreCase = true) }
            lower.contains("cine") || lower.contains("spotify") || lower.contains("netflix") || lower.contains("juego") ->
                categories.find { it.name.contains("Entretenimiento", ignoreCase = true) }
            else -> categories.find { it.name.contains("Otros", ignoreCase = true) }
        }
    }
}
