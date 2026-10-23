package com.presupuesto.cr.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.presupuesto.cr.domain.usecase.ParsedVoiceTransaction
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CurrencyFormatter

@Composable
fun VoiceConfirmSheet(
    parsedTransaction: ParsedVoiceTransaction?,
    onConfirm: (ParsedVoiceTransaction) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (parsedTransaction == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmar transacción",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "Descripción: ${parsedTransaction.description}")
                Text(text = "Categoría: ${parsedTransaction.categoryName}")
                Text(text = "Monto: ${CurrencyFormatter.formatCrc(parsedTransaction.amount)}")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(parsedTransaction) },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue)
            ) {
                Text(text = "Guardar", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}
