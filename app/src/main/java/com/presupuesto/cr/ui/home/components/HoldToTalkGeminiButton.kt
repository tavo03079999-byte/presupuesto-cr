package com.presupuesto.cr.ui.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HoldToTalkGeminiButton(
    isProcessing: Boolean = false,
    onTextCaptured: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.size(0.dp))
}
