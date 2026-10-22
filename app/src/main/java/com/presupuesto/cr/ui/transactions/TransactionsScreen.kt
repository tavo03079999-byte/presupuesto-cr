package com.presupuesto.cr.ui.transactions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.ui.theme.BcrNavy
import com.presupuesto.cr.ui.theme.EmeraldLight
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val transactions by viewModel.allTransactions.collectAsState()
    val recurringMovements by viewModel.recurringMovements.collectAsState()
    val pendingBcrList by viewModel.pendingBcrTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
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
                    text = { Text("Historial", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recurrentes", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bandeja BCR", fontWeight = FontWeight.SemiBold)
                            if (pendingBcrList.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = Color(0xFFE53935)) {
                                    Text("${pendingBcrList.size}")
                                }
                            }
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> HistoryTab(transactions = transactions, categories = categories)
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
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun HistoryTab(
    transactions: List<TransactionEntity>,
    categories: List<com.presupuesto.cr.data.local.entity.CategoryEntity>
) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay transacciones registradas aún.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                val category = categories.find { it.id == tx.categoryId }
                val amountColor = when (tx.type) {
                    CategoryType.INCOME -> EmeraldPrimary
                    CategoryType.EXPENSE -> Color(0xFFE53935)
                    CategoryType.SPECIAL_ALLOCATION -> Color(0xFFFBC02D)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tx.note.ifBlank { category?.name ?: "Transacción" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${category?.name ?: "General"}  •  ${DateUtils.formatFriendly(tx.timestamp)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = (if (tx.type == CategoryType.INCOME) "+ " else "- ") + CurrencyFormatter.formatCrc(tx.amount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = amountColor
                        )
                    }
                }
            }
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
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onTogglePaid(item) }) {
                            Icon(
                                imageVector = if (item.isPaidForCurrentPeriod) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.isPaidForCurrentPeriod) EmeraldPrimary else Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Día ${item.dayOfMonth} ${if (item.secondDayOfMonth != null) "y ${item.secondDayOfMonth}" else ""} • ${if (item.isPaidForCurrentPeriod) "Pagado este periodo" else "Pendiente de pago"}",
                                fontSize = 11.sp,
                                color = if (item.isPaidForCurrentPeriod) EmeraldPrimary else Color(0xFFE53935)
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
