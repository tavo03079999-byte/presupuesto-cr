package com.presupuesto.cr.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var zoomedImageContext by remember { mutableStateOf<Pair<TransactionEntity, String>?>(null) }

    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val recurringMovements by viewModel.recurringMovements.collectAsState()
    val pendingBcrList by viewModel.pendingBcrTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var minAmountInput by remember(filterState.minAmount) {
        mutableStateOf(filterState.minAmount?.toInt()?.toString() ?: "")
    }
    var maxAmountInput by remember(filterState.maxAmount) {
        mutableStateOf(filterState.maxAmount?.toInt()?.toString() ?: "")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    transactionToEdit = null
                    showAddDialog = true
                },
                containerColor = ThemeBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Transacción")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Movimientos",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Recurrentes",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bandeja BCR",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (pendingBcrList.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = ExpenseRed) {
                                    Text("${pendingBcrList.size}")
                                }
                            }
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = filterState.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        text = "Buscar descripción o comercio...",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = ThemeBlue)
                                },
                                trailingIcon = {
                                    if (filterState.searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            BadgedBox(
                                badge = {
                                    if (filterState.hasActiveFilters) {
                                        Badge(containerColor = ThemeBlue)
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { showAdvancedFilters = !showAdvancedFilters },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filtros",
                                        tint = if (filterState.hasActiveFilters) ThemeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = filterState.selectedType == null,
                                    onClick = { viewModel.updateTypeFilter(null) },
                                    label = { Text("Todos", fontSize = 12.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterState.selectedType == CategoryType.INCOME,
                                    onClick = { viewModel.updateTypeFilter(CategoryType.INCOME) },
                                    label = { Text("Ingresos", fontSize = 12.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterState.selectedType == CategoryType.EXPENSE,
                                    onClick = { viewModel.updateTypeFilter(CategoryType.EXPENSE) },
                                    label = { Text("Gastos", fontSize = 12.sp) }
                                )
                            }
                            if (filterState.hasActiveFilters) {
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            minAmountInput = ""
                                            maxAmountInput = ""
                                            viewModel.clearAllFilters()
                                        },
                                        label = { Text("Limpiar filtros ✕", fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showAdvancedFilters) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Filtrar por categoría:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            FilterChip(
                                                selected = filterState.selectedCategoryId == null,
                                                onClick = { viewModel.updateCategoryFilter(null) },
                                                label = { Text("Todas", fontSize = 11.sp) }
                                            )
                                        }
                                        items(categories, key = { it.id }) { cat ->
                                            FilterChip(
                                                selected = filterState.selectedCategoryId == cat.id,
                                                onClick = { viewModel.updateCategoryFilter(cat.id) },
                                                label = {
                                                    Text(
                                                        text = cat.name,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Rango de fechas:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { pickingStartDate = true },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = filterState.startDateMillis?.let { "Desde: ${DateUtils.formatDate(it)}" }
                                                    ?: "Fecha inicio",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { pickingEndDate = true },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = filterState.endDateMillis?.let { "Hasta: ${DateUtils.formatDate(it)}" }
                                                    ?: "Fecha fin",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = minAmountInput,
                                            onValueChange = {
                                                minAmountInput = it
                                                viewModel.updateAmountRangeFilter(
                                                    minAmount = it.toDoubleOrNull(),
                                                    maxAmount = maxAmountInput.toDoubleOrNull()
                                                )
                                            },
                                            label = { Text("Monto mín ₡", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = maxAmountInput,
                                            onValueChange = {
                                                maxAmountInput = it
                                                viewModel.updateAmountRangeFilter(
                                                    minAmount = minAmountInput.toDoubleOrNull(),
                                                    maxAmount = it.toDoubleOrNull()
                                                )
                                            },
                                            label = { Text("Monto máx ₡", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HistoryTab(
                        transactions = filteredTransactions,
                        categories = categories,
                        onEditClick = { tx ->
                            transactionToEdit = tx
                            showAddDialog = true
                        },
                        onDeleteClick = { tx ->
                            transactionToDelete = tx
                        },
                        onImageZoomClick = { tx, uri ->
                            zoomedImageContext = tx to uri
                        }
                    )
                }
                1 -> RecurringTab(
                    recurringMovements = recurringMovements,
                    onTogglePaid = { viewModel.toggleRecurringPaid(it) }
                )
                2 -> BcrInboxScreen(viewModel = viewModel)
            }
        }

        if (showAddDialog) {
            AddTransactionSheet(
                viewModel = viewModel,
                existingTransaction = transactionToEdit,
                onDismiss = {
                    showAddDialog = false
                    transactionToEdit = null
                }
            )
        }

        if (pickingStartDate) {
            val state = rememberDatePickerState(initialSelectedDateMillis = filterState.startDateMillis ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { pickingStartDate = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateDateRangeFilter(state.selectedDateMillis, filterState.endDateMillis)
                            pickingStartDate = false
                        }
                    ) { Text("Aplicar") }
                },
                dismissButton = {
                    TextButton(onClick = { pickingStartDate = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = state)
            }
        }

        if (pickingEndDate) {
            val state = rememberDatePickerState(initialSelectedDateMillis = filterState.endDateMillis ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { pickingEndDate = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateDateRangeFilter(filterState.startDateMillis, state.selectedDateMillis)
                            pickingEndDate = false
                        }
                    ) { Text("Aplicar") }
                },
                dismissButton = {
                    TextButton(onClick = { pickingEndDate = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = state)
            }
        }

        transactionToDelete?.let { tx ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Confirmar eliminación", fontWeight = FontWeight.Bold) },
                text = {
                    Text("¿Estás seguro de que deseas eliminar esta transacción (\"${tx.note.ifBlank { "Transacción" }}\" por ${CurrencyFormatter.formatCrc(tx.amount)})?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(tx)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { transactionToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        zoomedImageContext?.let { (tx, uriString) ->
            Dialog(onDismissRequest = { zoomedImageContext = null }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Comprobante Adjunto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = uriString,
                            contentDescription = "Imagen completa del comprobante",
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
                                    viewModel.removeSingleImageFromTransaction(tx, uriString)
                                    zoomedImageContext = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Eliminar foto", color = ExpenseRed)
                            }
                            Button(
                                onClick = { zoomedImageContext = null },
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
}

@Composable
fun HistoryTab(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteClick: (TransactionEntity) -> Unit,
    onImageZoomClick: (TransactionEntity, String) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No se encontraron transacciones con estos criterios.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                val category = categories.find { it.id == tx.categoryId }
                val catColor = CategoryIconMapper.parseColor(category?.colorHex ?: "#2196F3")
                val amountColor = when (tx.type) {
                    CategoryType.INCOME -> IncomeGreen
                    CategoryType.EXPENSE -> ExpenseRed
                    CategoryType.SPECIAL_ALLOCATION -> Color(0xFFFFC107)
                }
                val imageUris = TransactionsViewModel.parseImageUris(tx.receiptImagePath)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(catColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(category?.iconName ?: "Category"),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.note.ifBlank { category?.name ?: "Transacción" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${category?.name ?: "General"}  •  ${DateUtils.formatFriendly(tx.timestamp)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (tx.type == CategoryType.INCOME) "+" else "-") + CurrencyFormatter.formatCrc(tx.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = amountColor
                                )
                                Row {
                                    IconButton(
                                        onClick = { onEditClick(tx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = ThemeBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteClick(tx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (imageUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(imageUris) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Miniatura de comprobante",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onImageZoomClick(tx, uri) },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun RecurringTab(
    recurringMovements: List<RecurringMovementEntity>,
    onTogglePaid: (RecurringMovementEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(recurringMovements, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = { onTogglePaid(item) }) {
                            Icon(
                                imageVector = if (item.isPaidForCurrentPeriod) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.isPaidForCurrentPeriod) IncomeGreen else Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Día ${item.dayOfMonth} • ${if (item.isPaidForCurrentPeriod) "Pagado" else "Pendiente"}",
                                fontSize = 11.sp,
                                color = if (item.isPaidForCurrentPeriod) IncomeGreen else ExpenseRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = CurrencyFormatter.formatCrc(item.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
