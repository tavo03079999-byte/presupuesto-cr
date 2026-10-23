package com.presupuesto.cr.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.presupuesto.cr.BuildConfig
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ParsedVoiceTransaction(
    val amount: Double,
    val type: CategoryType,
    val categoryId: Long,
    val categoryName: String,
    val description: String
)

class ProcessGeminiVoiceUseCase(
    private val repository: FinanceRepository
) {

    companion object {
        private val GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY
    }

    suspend operator fun invoke(spokenText: String): Result<ParsedVoiceTransaction> =
        withContext(Dispatchers.IO) {
            try {
                val categories = repository.getAllCategories().first()
                if (GEMINI_API_KEY.isBlank()) {
                    return@withContext parseFallbackLocally(spokenText, categories.map { it.id to it.name })
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = GEMINI_API_KEY,
                    generationConfig = generationConfig {
                        temperature = 0.2f
                    }
                )

                val categoryNames = categories.joinToString(", ") { "${it.id}:${it.name}" }
                val prompt = """
                    Analiza el siguiente movimiento financiero en Costa Rica: "$spokenText".
                    Categorías disponibles (id:nombre): $categoryNames.
                    Responde únicamente un objeto JSON válido con las llaves:
                    {"amount": 0.0, "type": "EXPENSE", "categoryId": 3, "description": "texto"}
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val rawResponseText: String = (response.text ?: "{}").toString()
                val cleanJsonString: String = rawResponseText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                    .ifEmpty { "{}" }

                val jsonObject = JSONObject(cleanJsonString.toString())
                val amount = jsonObject.optDouble("amount", 0.0)
                val typeStr = jsonObject.optString("type", "EXPENSE")
                val categoryId = jsonObject.optLong("categoryId", categories.firstOrNull()?.id ?: 3L)
                val description = jsonObject.optString("description", spokenText)

                val resolvedType = if (typeStr.equals("INCOME", ignoreCase = true)) {
                    CategoryType.INCOME
                } else {
                    CategoryType.EXPENSE
                }
                val matchedCategoryName = categories.find { it.id == categoryId }?.name ?: "Otros"

                Result.success(
                    ParsedVoiceTransaction(
                        amount = amount,
                        type = resolvedType,
                        categoryId = categoryId,
                        categoryName = matchedCategoryName,
                        description = description
                    )
                )
            } catch (e: Exception) {
                Result.failure(Exception("No fue posible procesar el texto con inteligencia artificial."))
            }
        }

    private fun parseFallbackLocally(
        rawText: String,
        categories: List<Pair<Long, String>>
    ): Result<ParsedVoiceTransaction> {
        val numberRegex = Regex("""([0-9]+(?:[.,][0-9]+)?)""")
        val detectedAmount = numberRegex.find(rawText)?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull() ?: 0.0
        val lower = rawText.lowercase()
        val isIncome = lower.contains("ingreso") || lower.contains("salario") || lower.contains("pago recibido")
        val defaultCat = categories.firstOrNull() ?: (3L to "Alimentación")

        return Result.success(
            ParsedVoiceTransaction(
                amount = detectedAmount,
                type = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE,
                categoryId = defaultCat.first,
                categoryName = defaultCat.second,
                description = rawText.trim()
            )
        )
    }
}
