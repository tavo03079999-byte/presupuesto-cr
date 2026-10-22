package com.presupuesto.cr.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.presupuesto.cr.BuildConfig
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject

data class ParsedVoiceTransaction(
    val amount: Double,
    val type: CategoryType,
    val categoryId: Long,
    val categoryName: String,
    val accountId: Long,
    val accountName: String,
    val note: String,
    val merchant: String? = null
)

class ProcessGeminiVoiceUseCase(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(speechTranscript: String): Result<ParsedVoiceTransaction> {
        val categories = repository.getAllCategories().first()
        val accounts = repository.getAllAccounts().first()

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            // Fallback inteligente offline si no hay API key configurada
            return Result.success(fallbackLocalParsing(speechTranscript, categories, accounts))
        }

        return try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )

            val categoriesListStr = categories.joinToString(", ") { "${it.id}:${it.name} (${it.type})" }
            val accountsListStr = accounts.joinToString(", ") { "${it.id}:${it.name} (${it.accountType})" }

            val prompt = """
                Eres un asistente contable costarricense. Analiza la siguiente frase hablada por el usuario y extrae los datos de la transacción en formato JSON estricto.
                
                Moneda: Colones costarricenses (CRC / ₡).
                Nota sobre modismos ticos:
                - "15 mil colones" = 15000
                - "un rojo" = 1000
                - "cinco rojos" = 5000
                - "una teja" = 100
                - "un tucán" = 5000
                
                Categorías disponibles: [$categoriesListStr]
                Cuentas disponibles: [$accountsListStr]
                
                Frase del usuario: "$speechTranscript"
                
                Responde ÚNICAMENTE un objeto JSON válido con los siguientes campos sin formato markdown:
                {
                   "amount": número double (ej. 15000.0),
                   "type": "EXPENSE" o "INCOME" o "SPECIAL_ALLOCATION",
                   "categoryId": id numérico entero de la categoría sugerida,
                   "accountId": id numérico entero de la cuenta sugerida (si menciona tarjeta BCR usa la tarjeta BCR, si menciona efectivo usa Efectivo, si no especifica usa Cuenta BCR Débito),
                   "note": descripción breve y limpia del gasto,
                   "merchant": nombre del comercio si se menciona (ej. "Walmart", "Masxmenos", "Carnicería") o null
                }
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.trim()?.replace("```json", "")?.replace("```", "")?.trim()
                ?: throw IllegalStateException("Respuesta vacía de Gemini")

            val jsonObject = JSONObject(jsonText)
            val amount = jsonObject.optDouble("amount", 0.0)
            val typeStr = jsonObject.optString("type", "EXPENSE")
            val catId = jsonObject.optLong("categoryId", categories.firstOrNull()?.id ?: 1L)
            val accId = jsonObject.optLong("accountId", accounts.firstOrNull()?.id ?: 1L)
            val note = jsonObject.optString("note", speechTranscript)
            val merchant = if (jsonObject.has("merchant") && !jsonObject.isNull("merchant")) jsonObject.getString("merchant") else null

            val matchedCategory = categories.find { it.id == catId } ?: categories.first()
            val matchedAccount = accounts.find { it.id == accId } ?: accounts.first()

            Result.success(
                ParsedVoiceTransaction(
                    amount = amount,
                    type = CategoryType.valueOf(typeStr),
                    categoryId = matchedCategory.id,
                    categoryName = matchedCategory.name,
                    accountId = matchedAccount.id,
                    accountName = matchedAccount.name,
                    note = note,
                    merchant = merchant
                )
            )
        } catch (e: Exception) {
            // Si la llamada a Gemini falla (ej. sin conexión a internet), aplicar el parser heurístico local
            Result.success(fallbackLocalParsing(speechTranscript, categories, accounts))
        }
    }

    /**
     * Parser local heurístico para garantizar funcionamiento 100% offline
     */
    private fun fallbackLocalParsing(
        transcript: String,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>
    ): ParsedVoiceTransaction {
        val lower = transcript.lowercase()

        // 1. Extraer Monto
        var amount = 0.0
        val regexThousands = Regex("""(\d+)\s*(mil|k)""")
        val matchThousands = regexThousands.find(lower)
        if (matchThousands != null) {
            val num = matchThousands.groupValues[1].toDoubleOrNull() ?: 0.0
            amount = num * 1000.0
        } else {
            val regexPlain = Regex("""\b\d+([.,]\d+)?\b""")
            val matchPlain = regexPlain.find(lower)
            if (matchPlain != null) {
                amount = matchPlain.value.replace(",", ".").toDoubleOrNull() ?: 0.0
            }
        }
        if (amount == 0.0 && lower.contains("rojo")) amount = 1000.0
        if (amount == 0.0 && lower.contains("tucan") || lower.contains("tucán")) amount = 5000.0

        // 2. Extraer Categoría
        val category = when {
            lower.contains("carne") || lower.contains("pollo") || lower.contains("pescado") ->
                categories.find { it.name.contains("Carne", ignoreCase = true) }
            lower.contains("diezmo") ->
                categories.find { it.name.contains("Diezmo", ignoreCase = true) }
            lower.contains("super") || lower.contains("súper") || lower.contains("comida") || lower.contains("abarrotes") || lower.contains("walmart") || lower.contains("pali") || lower.contains("palí") ->
                categories.find { it.name.contains("Comida", ignoreCase = true) }
            lower.contains("gym") || lower.contains("gimnasio") ->
                categories.find { it.name.contains("Gym", ignoreCase = true) }
            lower.contains("agua") || lower.contains("luz") || lower.contains("internet") || lower.contains("kolbi") || lower.contains("servicio") ->
                categories.find { it.name.contains("Servicios", ignoreCase = true) }
            lower.contains("ahorro") || lower.contains("inversion") || lower.contains("inversión") ->
                categories.find { it.name.contains("Ahorro", ignoreCase = true) }
            lower.contains("salario") || lower.contains("quincena") ->
                categories.find { it.name.contains("Salario", ignoreCase = true) }
            else -> categories.find { it.type == CategoryType.EXPENSE } ?: categories.first()
        } ?: categories.first()

        // 3. Extraer Cuenta
        val account = when {
            lower.contains("tarjeta") || lower.contains("credito") || lower.contains("crédito") ->
                accounts.find { it.accountType == com.presupuesto.cr.data.local.entity.AccountType.CREDIT_CARD_BCR }
            lower.contains("efectivo") || lower.contains("plata") ->
                accounts.find { it.accountType == com.presupuesto.cr.data.local.entity.AccountType.CASH }
            else -> accounts.find { it.accountType == com.presupuesto.cr.data.local.entity.AccountType.BANK_BCR } ?: accounts.first()
        } ?: accounts.first()

        val type = if (category.isSpecialBalanceAllocation) {
            CategoryType.SPECIAL_ALLOCATION
        } else if (category.type == CategoryType.INCOME) {
            CategoryType.INCOME
        } else {
            CategoryType.EXPENSE
        }

        return ParsedVoiceTransaction(
            amount = if (amount > 0) amount else 5000.0,
            type = type,
            categoryId = category.id,
            categoryName = category.name,
            accountId = account.id,
            accountName = account.name,
            note = transcript.replaceFirstChar { it.uppercase() }
        )
    }
}
