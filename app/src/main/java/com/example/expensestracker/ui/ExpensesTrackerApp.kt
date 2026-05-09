package com.example.expensestracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensestracker.domain.CategoryTotal
import com.example.expensestracker.domain.MoneyTransaction
import com.example.expensestracker.domain.MonthlyStats
import com.example.expensestracker.domain.TransactionCategories
import com.example.expensestracker.domain.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private object Routes {
    const val List = "transactions"
    const val Stats = "stats"
    const val Detail = "detail/{id}"
    const val Edit = "edit/{type}/{id}"

    fun detail(id: Long) = "detail/$id"
    fun edit(type: TransactionType, id: Long = 0) = "edit/${type.name}/$id"
}

@Composable
fun ExpensesTrackerApp(viewModel: TrackerViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.List
    ) {
        composable(Routes.List) {
            TransactionListScreen(
                uiState = uiState,
                onOpenStats = { navController.navigate(Routes.Stats) },
                onOpenDetail = { navController.navigate(Routes.detail(it)) },
                onAdd = { navController.navigate(Routes.edit(it)) }
            )
        }
        composable(Routes.Stats) {
            StatsScreen(
                stats = uiState.monthlyStats,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0
            DetailScreen(
                transaction = uiState.transactions.firstOrNull { it.id == id },
                onBack = { navController.popBackStack() },
                onEdit = { transaction -> navController.navigate(Routes.edit(transaction.type, transaction.id)) },
                onDelete = {
                    viewModel.delete(id)
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.Edit,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0
            val routeType = entry.arguments?.getString("type").toTransactionType()
            TransactionEditScreen(
                transaction = uiState.transactions.firstOrNull { it.id == id },
                defaultType = routeType,
                onBack = { navController.popBackStack() },
                onSave = { transaction ->
                    viewModel.save(transaction)
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListScreen(
    uiState: TrackerUiState,
    onOpenStats: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onAdd: (TransactionType) -> Unit
) {
    var selectedTypeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    val selectedType = selectedTypeName.toTransactionType()
    val transactions = when (selectedType) {
        TransactionType.EXPENSE -> uiState.expenses
        TransactionType.INCOME -> uiState.incomes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Трекер расходов") },
                actions = {
                    TextButton(onClick = onOpenStats) {
                        Text("Статистика")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAdd(selectedType) }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryStrip(uiState)
            }
            item {
                TabRow(selectedTabIndex = selectedType.ordinal) {
                    Tab(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedTypeName = TransactionType.EXPENSE.name },
                        text = { Text("Расходы") }
                    )
                    Tab(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedTypeName = TransactionType.INCOME.name },
                        text = { Text("Доходы") }
                    )
                }
            }
            if (transactions.isEmpty()) {
                item {
                    EmptyState(type = selectedType)
                }
            } else {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onOpenDetail(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStrip(uiState: TrackerUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            title = "Баланс",
            amount = uiState.balanceCents,
            color = if (uiState.balanceCents >= 0) IncomeColor else ExpenseColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                title = "Доходы",
                amount = uiState.incomeCents,
                color = IncomeColor,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Расходы",
                amount = uiState.expenseCents,
                color = ExpenseColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatMoney(amount),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyState(type: TransactionType) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Нет записей: ${type.title.lowercase()}",
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: MoneyTransaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${transaction.category} • ${formatDisplayDate(transaction.dateMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatMoney(transaction.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = transaction.type.amountColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    transaction: MoneyTransaction?,
    onBack: () -> Unit,
    onEdit: (MoneyTransaction) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        if (transaction == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Запись не найдена")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatMoney(transaction.amountCents),
                        style = MaterialTheme.typography.headlineMedium,
                        color = transaction.type.amountColor(),
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    DetailLine("Тип", transaction.type.title)
                    DetailLine("Категория", transaction.category)
                    DetailLine("Дата", formatDisplayDate(transaction.dateMillis))
                    DetailLine("Заметка", transaction.note.ifBlank { "Нет" })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Редактировать")
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Удалить")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Операция будет удалена из локальной базы данных.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditScreen(
    transaction: MoneyTransaction?,
    defaultType: TransactionType,
    onBack: () -> Unit,
    onSave: (MoneyTransaction) -> Unit
) {
    val type = transaction?.type ?: defaultType
    val categories = TransactionCategories.forType(type)
    val formKey = transaction?.id ?: 0L
    var title by rememberSaveable(formKey) { mutableStateOf(transaction?.title.orEmpty()) }
    var amount by rememberSaveable(formKey) { mutableStateOf(transaction?.amountCents?.let(::formatAmountInput).orEmpty()) }
    var category by rememberSaveable(formKey) { mutableStateOf(transaction?.category ?: categories.first()) }
    var dateText by rememberSaveable(formKey) { mutableStateOf(transaction?.dateMillis?.let(::formatInputDate) ?: LocalDate.now().toString()) }
    var note by rememberSaveable(formKey) { mutableStateOf(transaction?.note.orEmpty()) }
    var error by rememberSaveable(formKey) { mutableStateOf<String?>(null) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transaction == null) "Новая запись" else "Редактирование") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = type.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Сумма") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Дата (ГГГГ-ММ-ДД)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val amountCents = parseAmountCents(amount)
                    val dateMillis = parseDateMillis(dateText)
                    error = when {
                        title.isBlank() -> "Введите название"
                        amountCents == null || amountCents <= 0 -> "Введите корректную сумму"
                        dateMillis == null -> "Введите дату в формате ГГГГ-ММ-ДД"
                        else -> null
                    }

                    if (error == null && amountCents != null && dateMillis != null) {
                        onSave(
                            MoneyTransaction(
                                id = transaction?.id ?: 0,
                                title = title.trim(),
                                amountCents = amountCents,
                                category = category,
                                type = type,
                                dateMillis = dateMillis,
                                note = note.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsScreen(
    stats: List<MonthlyStats>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stats.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Нет данных для статистики",
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(stats, key = { "${it.year}-${it.month}" }) { month ->
                    MonthlyStatsCard(month)
                }
            }
        }
    }
}

@Composable
private fun MonthlyStatsCard(stats: MonthlyStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = formatMonth(stats.year, stats.month),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Доходы", stats.incomeCents, IncomeColor, Modifier.weight(1f))
                Metric("Расходы", stats.expenseCents, ExpenseColor, Modifier.weight(1f))
                Metric("Итог", stats.balanceCents, stats.balanceCents.balanceColor(), Modifier.weight(1f))
            }
            HorizontalDivider()
            stats.categoryTotals.forEach { total ->
                CategoryTotalRow(total)
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CategoryTotalRow(total: CategoryTotal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${total.type.title}: ${total.category}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatMoney(total.amountCents),
            style = MaterialTheme.typography.bodyMedium,
            color = total.type.amountColor(),
            fontWeight = FontWeight.SemiBold
        )
    }
}

private val ExpenseColor = Color(0xFFB3261E)
private val IncomeColor = Color(0xFF1B7F3A)
private val InputDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val DisplayDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val MonthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru", "RU"))

private fun String?.toTransactionType(): TransactionType =
    runCatching { TransactionType.valueOf(this ?: TransactionType.EXPENSE.name) }
        .getOrDefault(TransactionType.EXPENSE)

private fun TransactionType.amountColor(): Color = when (this) {
    TransactionType.EXPENSE -> ExpenseColor
    TransactionType.INCOME -> IncomeColor
}

private fun Long.balanceColor(): Color = if (this >= 0) IncomeColor else ExpenseColor

private fun formatMoney(cents: Long): String {
    val amount = BigDecimal(cents).divide(BigDecimal(100))
    return NumberFormat.getCurrencyInstance(Locale("ru", "RU")).format(amount)
}

private fun formatAmountInput(cents: Long): String =
    BigDecimal(cents).divide(BigDecimal(100)).stripTrailingZeros().toPlainString()

private fun parseAmountCents(input: String): Long? =
    runCatching {
        BigDecimal(input.trim().replace(" ", "").replace(",", "."))
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()

private fun parseDateMillis(input: String): Long? =
    runCatching {
        LocalDate.parse(input.trim(), InputDateFormatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun formatInputDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(InputDateFormatter)

private fun formatDisplayDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DisplayDateFormatter)

private fun formatMonth(year: Int, month: Int): String {
    val value = YearMonth.of(year, month).format(MonthFormatter)
    return value.replaceFirstChar { it.uppercase(Locale("ru", "RU")) }
}
