package com.presupuesto.cr.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val crcSymbols = DecimalFormatSymbols(Locale("es", "CR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val currencyFormat = DecimalFormat("₡###,##0.00", crcSymbols)
    private val roundedCurrencyFormat = DecimalFormat("₡###,##0", crcSymbols)

    fun formatCrc(amount: Double, includeDecimals: Boolean = false): String {
        return if (includeDecimals) {
            currencyFormat.format(amount)
        } else {
            roundedCurrencyFormat.format(amount)
        }
    }

    fun parseCrc(text: String): Double? {
        val clean = text.replace("₡", "")
            .replace("CRC", "", ignoreCase = true)
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return clean.toDoubleOrNull()
    }
}
