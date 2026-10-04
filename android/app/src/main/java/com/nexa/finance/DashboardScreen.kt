package com.nexa.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexa.finance.ui.QuickEntryCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(api: ApiClient, logout: () -> Unit) {
    var accounts by remember { mutableStateOf(emptyList<Account>()) }
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var invoices by remember { mutableStateOf(emptyList<Invoice>()) }
    var categories by remember { mutableStateOf(emptyList<Category>()) }
    var cardName by remember { mutableStateOf("") }
    var cardBrand by remember { mutableStateOf("VISA") }
    var cardLastFour by remember { mutableStateOf("") }
    var cardLimit by remember { mutableStateOf("") }
    var cardClosingDay by remember { mutableStateOf("10") }
    var cardDueDay by remember { mutableStateOf("20") }
    var cardLiabilityAccountId by remember { mutableStateOf("") }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var transactions by remember { mutableStateOf(emptyList<Tx>()) }
    var recurrences by remember { mutableStateOf(emptyList<Recurrence>()) }
    var budgets by remember { mutableStateOf(emptyList<Budget>()) }
    var message by remember { mutableStateOf("Carregando...") }
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("CHECKING") }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var transferSourceId by remember { mutableStateOf("") }
    var transferDestinationId by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }

    fun refresh() {
        Thread {
            var failures = 0
            runCatching { api.accounts() }.onSuccess { runOnMain { accounts = it } }.onFailure { failures++ }
            runCatching { api.cards() }.onSuccess { runOnMain { cards = it } }.onFailure { failures++ }
            runCatching { api.invoices() }.onSuccess { runOnMain { invoices = it } }.onFailure { failures++ }
            runCatching { api.categories() }.onSuccess { runOnMain { categories = it } }.onFailure { failures++ }
            runCatching { api.transactions() }.onSuccess { runOnMain { transactions = it } }.onFailure { failures++ }
            runCatching { api.recurrences() }.onSuccess { runOnMain { recurrences = it } }.onFailure { failures++ }
            runCatching { api.budgets() }.onSuccess { runOnMain { budgets = it } }.onFailure { failures++ }
            runOnMain { message = if (failures == 0) "" else "Alguns dados não puderam ser carregados." }
        }.start()
    }

    LaunchedEffect(Unit) { refresh() }

    val month = YearMonth.now()
    val previousMonth = month.minusMonths(1)
    val current = transactions.filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) == month }.getOrDefault(false) }
    val previous = transactions.filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) == previousMonth }.getOrDefault(false) }
    val income = current.filter { it.direction == "CREDIT" }.sumOf { it.amountMinor }
    val expenses = current.filter { it.direction == "DEBIT" }.sumOf { it.amountMinor }
    val previousExpenses = previous.filter { it.direction == "DEBIT" }.sumOf { it.amountMinor }
    val categoryTotals = current.filter { it.direction == "DEBIT" }
        .groupBy { tx -> categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Sem categoria" }
        .mapValues { it.value.sumOf(Tx::amountMinor) }.toList().sortedByDescending { it.second }
    val activeRecurrences = recurrences.filter { it.status == "ACTIVE" }

    Scaffold(topBar = {
        TopAppBar(title = { Text("NEXA Finance") }, actions = { TextButton(onClick = logout) { Text("Sair") } })
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().testTag("dashboard_list").padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Visão financeira", style = MaterialTheme.typography.headlineSmall)
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR")))
                        .replaceFirstChar { it.uppercase() }
                )
            }
            item {
                Card(Modifier.fillMaxWidth().testTag("dashboard_balance")) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Saldo total", style = MaterialTheme.typography.labelLarge)
                        Text(formatMoney(accounts.sumOf { it.balanceMinor }), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetric("Entradas", income, Modifier.weight(1f).testTag("dashboard_income"))
                    DashboardMetric("Despesas", expenses, Modifier.weight(1f).testTag("dashboard_expenses"))
                }
            }
            item {
                DashboardPanel("Resultado do período", Modifier.testTag("dashboard_result")) {
                    Text(formatMoney(income - expenses), style = MaterialTheme.typography.titleLarge)
                    val delta = if (previousExpenses == 0L) null else (expenses - previousExpenses).toDouble() / previousExpenses * 100
                    Text(delta?.let { "Despesas " + if (it <= 0) "↓" else "↑" + " " + String.format(Locale.US, "%.1f", abs(it)) + "% vs. mês anterior" }
                        ?: "Sem base de comparação para o mês anterior")
                }
            }
            item {
                DashboardPanel("Gastos por categoria", Modifier.testTag("dashboard_categories")) {
                    if (categoryTotals.isEmpty()) Text("Nenhuma despesa categorizada neste período.")
                    else categoryTotals.take(6).forEach { (name, total) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, Modifier.weight(1f))
                            Text(formatMoney(total))
                        }
                    }
                }
            }
            item {
                DashboardPanel("Evolução temporal", Modifier.testTag("dashboard_evolution")) {
                    val weekly = (0..3).map { week ->
                        val start = month.atDay(1).plusDays((week * 7).toLong())
                        val end = if (week == 3) month.atEndOfMonth() else start.plusDays(6)
                        current.filter {
                            runCatching { LocalDate.parse(it.date) in start..end }.getOrDefault(false) && it.direction == "DEBIT"
                        }.sumOf { it.amountMinor }
                    }
                    weekly.forEachIndexed { index, total ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Semana " + (index + 1))
                            Text(formatMoney(total))
                        }
                    }
                }
            }
            item {
                DashboardPanel("Gastos recorrentes", Modifier.testTag("dashboard_recurring")) {
                    if (activeRecurrences.isEmpty()) Text("Nenhuma recorrência ativa.")
                    else activeRecurrences.take(5).forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(it.description)
                                Text(it.frequency.lowercase(Locale.ROOT) + " · a partir de " + it.startOn, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatMoney(it.amountMinor))
                        }
                    }
                }
            }
            item {
                BudgetPanel(
                    api = api,
                    budgets = budgets,
                    categories = categories,
                    onBudgetsChanged = { budgets = it },
                    onMessage = { message = it },
                )
            }
            item {
                DashboardPanel("Insight", Modifier.testTag("dashboard_insight")) {
                    val top = categoryTotals.firstOrNull()
                    Text(
                        when {
                            top != null -> "Sua maior categoria de despesa neste mês é " + top.first + ", com " + formatMoney(top.second) + "."
                            current.isEmpty() -> "Ainda não há lançamentos suficientes neste mês para gerar um insight."
                            else -> "Continue registrando seus lançamentos para enriquecer seus insights."
                        }
                    )
                }
            }
            item {
                DashboardPanel("Cartões e faturas", Modifier.testTag("cards_invoices")) {
                    if (cards.isEmpty()) {
                        Text("Nenhum cartão cadastrado.")
                    } else {
                        cards.forEach { card ->
                            val cardInvoices = invoices.filter { it.cardId == card.id }.sortedByDescending { it.periodEnd }
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("card_" + card.id)) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(card.name, style = MaterialTheme.typography.titleMedium)
                                            Text(card.brand + " •••• " + card.lastFourDigits)
                                            Text("Limite disponível: " + formatMoney(card.availableLimitMinor))
                                            Text("Limite total: " + formatMoney(card.creditLimitMinor), style = MaterialTheme.typography.bodySmall)
                                            Text("Fecha dia " + card.closingDay + " • vence dia " + card.dueDay, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(card.status)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { selectedCardId = if (selectedCardId == card.id) null else card.id }) {
                                            Text(if (selectedCardId == card.id) "Fechar" else "Detalhes")
                                        }
                                        TextButton(onClick = {
                                            val currentMonth = YearMonth.now()
                                            Thread {
                                                runCatching {
                                                    api.createInvoice(card.id, currentMonth.atDay(1).toString(), currentMonth.atEndOfMonth().toString(), currentMonth.atEndOfMonth().plusDays(7).toString())
                                                }.onSuccess { created ->
                                                    runOnMain { invoices = listOf(created) + invoices; message = "Fatura criada." }
                                                }.onFailure { e -> runOnMain { message = e.message ?: "Não foi possível criar a fatura." } }
                                            }.start()
                                        }) { Text("Gerar fatura") }
                                    }
                                    if (selectedCardId == card.id) {
                                        Text("Histórico", style = MaterialTheme.typography.labelLarge)
                                        val cardTx = transactions.filter { it.cardId == card.id }.take(10)
                                        if (cardTx.isEmpty()) Text("Nenhuma movimentação.")
                                        else cardTx.forEach { tx ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(tx.description, Modifier.weight(1f))
                                                Text(formatMoney(tx.amountMinor))
                                            }
                                        }
                                        Text("Faturas", style = MaterialTheme.typography.labelLarge)
                                        if (cardInvoices.isEmpty()) Text("Nenhuma fatura gerada.")
                                        else cardInvoices.take(4).forEach { invoice ->
                                            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(invoice.periodStart + " → " + invoice.periodEnd)
                                                    Text(invoice.status)
                                                }
                                                Text("Total: " + formatMoney(invoice.totalAmountMinor) + " • Restante: " + formatMoney(invoice.remainingAmountMinor))
                                                Text("Vencimento: " + invoice.dueOn, style = MaterialTheme.typography.bodySmall)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (invoice.status == "OPEN") {
                                                        OutlinedButton(onClick = {
                                                            Thread {
                                                                runCatching { api.closeInvoice(invoice.id) }
                                                                    .onSuccess { updated -> runOnMain { invoices = invoices.map { if (it.id == updated.id) updated else it }; message = "Fatura fechada." } }
                                                                    .onFailure { e -> runOnMain { message = e.message ?: "Erro ao fechar fatura." } }
                                                            }.start()
                                                        }) { Text("Fechar") }
                                                    }
                                                    if ((invoice.status == "CLOSED" || invoice.status == "OVERDUE") && invoice.remainingAmountMinor > 0 && accounts.any { it.type != "CREDIT_CARD" }) {
                                                        TextButton(onClick = {
                                                            val source = accounts.firstOrNull { it.type == "CHECKING" || it.type == "SAVINGS" || it.type == "CASH" }
                                                            if (source == null) {
                                                                message = "Crie uma conta de origem para pagar a fatura."
                                                            } else {
                                                                Thread {
                                                                    runCatching { api.payInvoice(invoice.id, source.id, invoice.remainingAmountMinor) }
                                                                        .onSuccess { updated -> runOnMain { invoices = invoices.map { if (it.id == updated.id) updated else it }; message = "Fatura paga." } }
                                                                        .onFailure { e -> runOnMain { message = e.message ?: "Erro ao pagar fatura." } }
                                                                }.start()
                                                            }
                                                        }) { Text("Pagar") }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Novo cartão", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(cardName, { cardName = it }, Modifier.fillMaxWidth().testTag("card_name"), label = { Text("Nome") })
                    OutlinedTextField(cardLastFour, { cardLastFour = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth().testTag("card_last_four"), label = { Text("Últimos 4 dígitos") })
                    OutlinedTextField(cardLimit, { cardLimit = it }, Modifier.fillMaxWidth().testTag("card_limit"), label = { Text("Limite (R$)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = cardBrand == "VISA", onClick = { cardBrand = "VISA" }, label = { Text("Visa") })
                        FilterChip(selected = cardBrand == "MASTERCARD", onClick = { cardBrand = "MASTERCARD" }, label = { Text("Mastercard") })
                        FilterChip(selected = cardBrand == "ELO", onClick = { cardBrand = "ELO" }, label = { Text("Elo") })
                    }
                    Text("Conta de passivo do cartão", style = MaterialTheme.typography.labelMedium)
                    accounts.filter { it.type == "CREDIT_CARD" }.forEach { account ->
                        FilterChip(selected = cardLiabilityAccountId == account.id, onClick = { cardLiabilityAccountId = account.id }, label = { Text(account.name) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(cardClosingDay, { cardClosingDay = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("Fecha") })
                        OutlinedTextField(cardDueDay, { cardDueDay = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("Vence") })
                    }
                    Button(
                        enabled = cardName.isNotBlank() && cardLastFour.length == 4 &&
                            cardLimit.replace(",", ".").toDoubleOrNull()?.let { it > 0 } == true &&
                            cardLiabilityAccountId.isNotBlank(),
                        modifier = Modifier.testTag("card_submit"),
                        onClick = {
                            val limitMinor = ((cardLimit.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toLong()
                            Thread {
                                runCatching {
                                    api.createCard(cardLiabilityAccountId, cardName, cardBrand, cardLastFour, limitMinor, cardClosingDay.toIntOrNull() ?: 10, cardDueDay.toIntOrNull() ?: 20)
                                }.onSuccess { created ->
                                    runOnMain {
                                        cards = cards + created
                                        cardName = ""; cardLastFour = ""; cardLimit = ""
                                        message = "Cartão cadastrado."
                                    }
                                }.onFailure { e -> runOnMain { message = e.message ?: "Erro ao cadastrar cartão." } }
                            }.start()
                        }
                    ) { Text("Cadastrar cartão") }
                }
            }
            item {
                QuickEntryCard(
                    api = api,
                    accounts = accounts,
                    cards = cards,
                    categories = categories,
                    onTransactionAdded = { transactions = listOf(it) + transactions },
                    onMessage = { message = it }
                )
            }
            item {
                DashboardPanel("Transferir entre contas", Modifier.testTag("account_transfer")) {
                    if (accounts.size < 2) {
                        Text("Crie pelo menos duas contas para transferir valores.")
                    } else {
                        Text("Origem")
                        accounts.forEach { account ->
                            FilterChip(
                                selected = transferSourceId == account.id,
                                onClick = { transferSourceId = account.id },
                                label = { Text(account.name) }
                            )
                        }
                        Text("Destino")
                        accounts.filter { it.id != transferSourceId }.forEach { account ->
                            FilterChip(
                                selected = transferDestinationId == account.id,
                                onClick = { transferDestinationId = account.id },
                                label = { Text(account.name) }
                            )
                        }
                        OutlinedTextField(
                            transferAmount,
                            { transferAmount = it },
                            Modifier.fillMaxWidth().testTag("transfer_amount"),
                            label = { Text("Valor (R$)") }
                        )
                        Button(
                            enabled = transferSourceId.isNotBlank() &&
                                transferDestinationId.isNotBlank() &&
                                transferSourceId != transferDestinationId &&
                                transferAmount.replace(",", ".").toDoubleOrNull()?.let { it > 0 } == true,
                            onClick = {
                                val cents = ((transferAmount.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toLong()
                                Thread {
                                    try {
                                        api.createTransfer(transferSourceId, transferDestinationId, cents)
                                        val latest = api.accounts()
                                        val latestTx = api.transactions()
                                        runOnMain {
                                            accounts = latest
                                            transactions = latestTx
                                            transferAmount = ""
                                            message = "Transferência registrada."
                                        }
                                    } catch (e: Exception) {
                                        runOnMain { message = e.message ?: "Erro na transferência." }
                                    }
                                }.start()
                            },
                            modifier = Modifier.testTag("transfer_submit")
                        ) { Text("Transferir") }
                    }
                }
            }
            item { Text("Contas", style = MaterialTheme.typography.titleLarge) }
            items(accounts) { account ->
                Card(Modifier.fillMaxWidth().testTag("account_" + account.id)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium)
                                Text(account.type.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() } + " • " + account.currency, style = MaterialTheme.typography.bodySmall)
                                Text(formatMoney(account.balanceMinor))
                            }
                            if (account.isPrimary) Text("Principal", style = MaterialTheme.typography.labelMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedAccountId = account.id }) { Text("Detalhes") }
                            if (!account.isPrimary) {
                                OutlinedButton(onClick = {
                                    Thread {
                                        try {
                                            api.setPrimaryAccount(account.id)
                                            val latest = api.accounts()
                                            runOnMain { accounts = latest; message = "Conta principal atualizada." }
                                        } catch (e: Exception) {
                                            runOnMain { message = e.message ?: "Erro" }
                                        }
                                    }.start()
                                }) { Text("Tornar principal") }
                            }
                        }
                    }
                }
            }
            selectedAccountId?.let { id ->
                accounts.firstOrNull { it.id == id }?.let { account ->
                    item {
                        DashboardPanel("Detalhes da conta", Modifier.testTag("account_details")) {
                            Text(account.name, style = MaterialTheme.typography.titleMedium)
                            Text("Tipo: " + account.type)
                            Text("Moeda: " + account.currency)
                            Text("Saldo: " + formatMoney(account.balanceMinor))
                            Text(if (account.isPrimary) "Esta é a conta principal." else "Conta secundária.")
                            TextButton(onClick = { selectedAccountId = null }) { Text("Fechar detalhes") }
                        }
                    }
                    item {
                        DashboardPanel("Histórico da conta", Modifier.testTag("account_history")) {
                            val accountTx = transactions.filter { it.accountId == account.id }
                            if (accountTx.isEmpty()) Text("Nenhuma movimentação encontrada.")
                            else accountTx.take(20).forEach { tx ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(tx.description)
                                        Text(tx.date, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text((if (tx.direction == "DEBIT") "-" else "+") + formatMoney(tx.amountMinor))
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    accountName,
                    { accountName = it },
                    Modifier.fillMaxWidth().testTag("account_name"),
                    label = { Text("Nova conta") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = accountType == "CHECKING", onClick = { accountType = "CHECKING" }, label = { Text("Corrente") })
                    FilterChip(selected = accountType == "SAVINGS", onClick = { accountType = "SAVINGS" }, label = { Text("Poupança") })
                    FilterChip(selected = accountType == "CASH", onClick = { accountType = "CASH" }, label = { Text("Dinheiro") })
                    FilterChip(selected = accountType == "CREDIT_CARD", onClick = { accountType = "CREDIT_CARD" }, label = { Text("Cartão") })
                }
                Button(
                    modifier = Modifier.testTag("account_submit"),
                    enabled = accountName.isNotBlank(),
                    onClick = {
                        Thread {
                            try {
                                val created = api.createAccount(accountName, accountType)
                                runOnMain { accounts = accounts + created; accountName = ""; message = "Conta criada." }
                            } catch (e: Exception) {
                                runOnMain { message = e.message ?: "Erro" }
                            }
                        }.start()
                    }
                ) { Text("Adicionar conta") }
            }
            if (message.isNotBlank()) item { Text(message, modifier = Modifier.testTag("dashboard_message")) }
        }
    }
}

@Composable
private fun DashboardMetric(title: String, amountMinor: Long, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(formatMoney(amountMinor), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DashboardPanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

private fun formatMoney(minor: Long): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", minor / 100.0)

private fun runOnMain(action: () -> Unit) =
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
