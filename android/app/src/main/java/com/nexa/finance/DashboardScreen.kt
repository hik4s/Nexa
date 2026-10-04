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
    var categories by remember { mutableStateOf(emptyList<Category>()) }
    var transactions by remember { mutableStateOf(emptyList<Tx>()) }
    var recurrences by remember { mutableStateOf(emptyList<Recurrence>()) }
    var message by remember { mutableStateOf("Carregando...") }
    var accountName by remember { mutableStateOf("") }

    fun refresh() {
        Thread {
            var failures = 0
            runCatching { api.accounts() }.onSuccess { runOnMain { accounts = it } }.onFailure { failures++ }
            runCatching { api.cards() }.onSuccess { runOnMain { cards = it } }.onFailure { failures++ }
            runCatching { api.categories() }.onSuccess { runOnMain { categories = it } }.onFailure { failures++ }
            runCatching { api.transactions() }.onSuccess { runOnMain { transactions = it } }.onFailure { failures++ }
            runCatching { api.recurrences() }.onSuccess { runOnMain { recurrences = it } }.onFailure { failures++ }
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
                DashboardPanel("Orçamento consumido", Modifier.testTag("dashboard_budget")) {
                    Text("Nenhum orçamento configurado ainda.")
                    Text("O módulo de orçamento será habilitado na etapa 2.5.", style = MaterialTheme.typography.bodySmall)
                }
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
                QuickEntryCard(
                    api = api,
                    accounts = accounts,
                    cards = cards,
                    categories = categories,
                    onTransactionAdded = { transactions = listOf(it) + transactions },
                    onMessage = { message = it }
                )
            }
            item { Text("Contas", style = MaterialTheme.typography.titleLarge) }
            items(accounts) { account ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        Text(formatMoney(account.balanceMinor))
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
                Button(
                    modifier = Modifier.testTag("account_submit"),
                    enabled = accountName.isNotBlank(),
                    onClick = {
                        Thread {
                            try {
                                val created = api.createAccount(accountName)
                                runOnMain { accounts = accounts + created; accountName = "" }
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
