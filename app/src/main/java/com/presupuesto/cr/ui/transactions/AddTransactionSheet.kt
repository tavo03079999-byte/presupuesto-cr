package com.presupuesto.cr.ui.transactions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.data.local.entity.TransactionSource
import com.presupuesto.cr.ui.theme.CyanNeon
import com.presupuesto.cr.ui.theme.EmeraldNeon
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.DateUtils
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    viewModel: TransactionsViewModel,
    existingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isScanningOcr by viewModel.isScanningOcr.collectAsState()
    val scannedReceipt by viewModel.scannedReceiptResult.collectAsState()

    var amountText by remember(existingTransaction) {
        mutableStateOf(
            if (existingTransaction != null) {
                val longVal = existingTransaction.amount.toLong()
                if (existingTransaction.amount == longVal.toDouble()) longVal.toString()
                else existingTransaction.amount.toString()
            } else {
                ""
            }
        )
    }
    var noteText by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.note ?: "")
    }
    var selectedType by remember(existingTransaction) {
        mutableStateOf(
            if (existingTransaction?.type == CategoryType.INCOME) CategoryType.INCOME
            else CategoryType.EXPENSE
        )
    }
    var selectedTimestamp by remember(existingTransaction) {
        mutableLongStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var selectedAccountId by remember(existingTransaction, accounts) {
        mutableLongStateOf(existingTransaction?.accountId ?: accounts.firstOrNull()?.id ?: 1L)
    }

    val matchingCategories = categories.filter { it.type == selectedType }
    var selectedCategoryId by remember(existingTransaction, selectedType, categories) {
        val initial = existingTransaction?.categoryId?.takeIf { id ->
            matchingCategories.any { it.id == id }
        } ?: matchingCategories.firstOrNull()?.id ?: categories.firstOrNull()?.id ?: 1L
        mutableLongStateOf(initial)
    }

    var merchantText by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.merchant ?: "")
    }

    val attachedImageUris = remember(existingTransaction) {
        mutableStateListOf<String>().apply {
            addAll(TransactionsViewModel.parseImageUris(existingTransaction?.receiptImagePath))
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedUri = saveBitmapToLocalStorage(context, bitmap)
            if (savedUri != null) {
                attachedImageUris.add(savedUri)
            }
            viewModel.processReceiptImage(bitmap)
        }
    }

    val multipleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val copiedUri = copyContentUriToLocalStorage(context, uri)
            if (copiedUri != null) {
                attachedImageUris.add(copiedUri)
            }
        }
    }

    LaunchedEffect(scannedReceipt) {
        scannedReceipt?.let { data ->
            if (data.totalAmount > 0) {
                amountText = data.totalAmount.toInt().toString()
            }
            if (data.merchant != null) {
                merchantText = data.merchant
                if (noteText.isBlank()) {
                    noteText = "Factura: ${data.merchant}"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                        Text(
                            text = if (existingTransaction == null) "Nueva transacción" else "Editar transacción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isIncome = selectedType == CategoryType.INCOME
                        Surface(
                            onClick = { selectedType = CategoryType.INCOME },
                            shape = RoundedCornerShape(16.dp),
                            border = if (isIncome) BorderStroke(1.dp, IncomeGreen) else null,
                            color = if (isIncome) IncomeGreen.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ingreso",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isExpense = selectedType == CategoryType.EXPENSE
                        Surface(
                            onClick = { selectedType = CategoryType.EXPENSE },
                            shape = RoundedCornerShape(16.dp),
                            border = if (isExpense) BorderStroke(1.dp, ExpenseRed) else null,
                            color = if (isExpense) ExpenseRed.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isExpense) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gasto",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpense) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto (₡ CRC)") },
                        prefix = {
                            Text(
                                text = "₡ ",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == CategoryType.INCOME) IncomeGreen else ExpenseRed
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = ThemeBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Categoría",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val listToShow = matchingCategories.ifEmpty { categories }
                            items(listToShow, key = { it.id }) { cat ->
                                val catColor = CategoryIconMapper.parseColor(cat.colorHex)
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = { selectedCategoryId = cat.id },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconMapper.getIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = cat.name,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = ThemeBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Fecha",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = DateUtils.formatFriendly(selectedTimestamp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Text(
                                text = "Cambiar",
                                fontSize = 12.sp,
                                color = ThemeBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Descripción (Ej. Salario, supermercado...)") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null, tint = ThemeBlue)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = ThemeBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Método de pago / Cuenta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accounts, key = { it.id }) { acc ->
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id },
                                    label = {
                                        Text(
                                            text = acc.name,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Comprobantes / Facturas adjuntas (${attachedImageUris.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { multipleGalleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galería", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { cameraLauncher.launch() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isScanningOcr) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cámara / OCR", fontSize = 12.sp)
                            }
                        }

                        if (attachedImageUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(attachedImageUris.toList()) { uriStr ->
                                    Box(modifier = Modifier.size(68.dp)) {
                                        AsyncImage(
                                            model = uriStr,
                                            contentDescription = "Factura adjunta",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { previewImageUri = uriStr },
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .background(ExpenseRed, CircleShape)
                                                .clickable { attachedImageUris.remove(uriStr) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Eliminar imagen",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(ThemeBlue, CyanNeon, EmeraldNeon)
                                ),
                                shape = CircleShape
                            )
                            .clickable {
                                val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    if (existingTransaction == null) {
                                        viewModel.addManualTransaction(
                                            accountId = selectedAccountId,
                                            categoryId = selectedCategoryId,
                                            amount = amount,
                                            type = selectedType,
                                            timestamp = selectedTimestamp,
                                            note = noteText.ifBlank {
                                                categories.find { it.id == selectedCategoryId }?.name ?: "Movimiento"
                                            },
                                            merchant = merchantText.ifBlank { null },
                                            receiptUris = attachedImageUris.toList(),
                                            source = if (scannedReceipt != null) TransactionSource.OCR_RECEIPT else TransactionSource.MANUAL
                                        )
                                    } else {
                                        viewModel.updateExistingTransaction(
                                            oldTransaction = existingTransaction,
                                            accountId = selectedAccountId,
                                            categoryId = selectedCategoryId,
                                            amount = amount,
                                            type = selectedType,
                                            timestamp = selectedTimestamp,
                                            note = noteText.ifBlank { "Movimiento editado" },
                                            merchant = merchantText.ifBlank { null },
                                            receiptUris = attachedImageUris.toList()
                                        )
                                    }
                                    viewModel.clearScannedReceipt()
                                    onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (existingTransaction == null) "Guardar" else "Actualizar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedTimestamp = millis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    previewImageUri?.let { uriStr ->
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = uriStr,
                        contentDescription = "Vista completa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                attachedImageUris.remove(uriStr)
                                previewImageUri = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Eliminar", color = ExpenseRed)
                        }
                        Button(
                            onClick = { previewImageUri = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }
}

private fun saveBitmapToLocalStorage(context: Context, bitmap: Bitmap): String? {
    return try {
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        null
    }
}

private fun copyContentUriToLocalStorage(context: Context, sourceUri: Uri): String? {
    return try {
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(receiptsDir, "gallery_${System.currentTimeMillis()}_${sourceUri.hashCode()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        sourceUri.toString()
    }
}
