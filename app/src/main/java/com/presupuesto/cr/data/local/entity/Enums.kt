package com.presupuesto.cr.data.local.entity

enum class AccountType {
    CASH,               // Efectivo
    BANK_BCR,           // Cuenta Bancaria BCR (Débito/Ahorros)
    CREDIT_CARD_BCR,    // Tarjeta de Crédito BCR
    SAVINGS,            // Cuentas de Ahorro / Metas
    INVESTMENT          // Inversiones
}

enum class CategoryType {
    INCOME,             // Ingreso (Salario, Cobros)
    EXPENSE,            // Gasto (Comida, Servicios, etc.)
    SPECIAL_ALLOCATION  // Asignación especial (Ahorro / Inversión)
}

enum class RecurringFrequency {
    WEEKLY,
    FORTNIGHTLY_15_30,  // Típico ciclo quincenal en Costa Rica
    MONTHLY,
    ANNUAL
}

enum class BudgetPeriod {
    WEEKLY,
    FORTNIGHTLY,
    MONTHLY,
    ANNUAL
}

enum class TransactionSource {
    MANUAL,
    VOICE_GEMINI,
    OCR_RECEIPT,
    BCR_NOTIFICATION
}
