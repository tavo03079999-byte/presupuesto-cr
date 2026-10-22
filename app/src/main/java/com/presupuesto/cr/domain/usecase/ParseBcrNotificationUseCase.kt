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

        // Verificar si es una notificación del Banco de Costa Rica
        val isBcr = fullText.contains("BCR", ignoreCase = true) ||
                fullText.contains("Banco de Costa Rica", ignoreCase = true) ||
                fullText.contains("SINPE", ignoreCase = true)

        if (!isBcr) return null

        // 1. Extraer Monto en Colones
        // Formatos típicos: "CRC 18,500.00", "₡18.500", "monto de 18,500.00", "por ₡25,000"
        var parsedAmount = 0.0
        val amountRegex = Regex("""(?:CRC|₡|monto(?:\s+de)?|por)\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(fullText)
        if (amountMatch != null) {
            val rawNum = amountMatch.groupValues[1]
                .replace(".", "")
                .replace(",", ".")
            parsedAmount = rawNum.toDoubleOrNull() ?: 0.0
        }

        // Si no se detectó monto, buscar cualquier número significativo
        if (parsedAmount == 0.0) {
            val generalNumberRegex = Regex("""\b([0-9]{1,3}(?:[.,][0-9]{3})+)\b""")
            val generalMatch = generalNumberRegex.find(fullText)
            if (generalMatch != null) {
                val rawNum = generalMatch.groupValues[1].replace(".", "").replace(",", "")
                parsedAmount = rawNum.toDoubleOrNull() ?: 0.0
            }
        }

        if (parsedAmount <= 0.0) return null

        // 2. Extraer Comercio o Destinatario
        var merchant: String? = null
        val merchantRegex = Regex("""(?:en|comercio|en el comercio|a)\s+([A-Za-z0-9\s\.\-]{3,30}?)(?:\s+(?:el|con|por|tarjeta|fecha)|$)""", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(fullText)
        if (merchantMatch != null) {
            merchant = merchantMatch.groupValues[1].trim()
        }

        // 3. Extraer Terminación de Tarjeta
        var cardLast4: String? = null
        val cardRegex = Regex("""(?:tarjeta|cta|cuenta|\*)\s*(\d{4})""", RegexOption.IGNORE_CASE)
        val cardMatch = cardRegex.find(fullText)
        if (cardMatch != null) {
            cardLast4 = "*${cardMatch.groupValues[1]}"
        }

        // 4. Sugerir Categoría inteligente en base al comercio
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
            lower.contains("carne") || lower.contains("carniceria") || lower.contains("carnicería") || lower.contains("beef") ->
                categories.find { it.name.contains("Carne", ignoreCase = true) }
            lower.contains("super") || lower.contains("súper") || lower.contains("walmart") || lower.contains("automercado") || lower.contains("pali") || lower.contains("palí") || lower.contains("masxmenos") ->
                categories.find { it.name.contains("Comida", ignoreCase = true) }
            lower.contains("ice") || lower.contains("kolbi") || lower.contains("kölbi") || lower.contains("cnfl") || lower.contains("aya") || lower.contains("esph") || lower.contains("telecable") || lower.contains("liberty") ->
                categories.find { it.name.contains("Servicios", ignoreCase = true) }
            lower.contains("smart") || lower.contains("gym") || lower.contains("fitness") || lower.contains("gimnasio") ->
                categories.find { it.name.contains("Gym", ignoreCase = true) }
            else -> null
        }
    }
}
