package com.nexa.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Locale

@Composable
fun BudgetPanel(
    api: ApiClient,
    budgets: List<Budget>,
    categories: List<Category>,
    onBudgetsChanged: (List<Budget>) -> Unit,
    onMessage: (String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    val month = YearMonth.now()
    val startOn = month.atDay(1).toString()
    val endOn = month.atEndOfMonth().toString()
    val currentBudgets = budgets.filter { it.status != "CANCELLED" && it.startOn <= endOn && it.endOn >= startOn }
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Todas as categorias"

    fun refresh() {
        Thread {
            runCatching { api.budgets() }
                .onSuccess { latest -> budgetRunOnMain { onBudgetsChanged(latest) } }
                .onFailure { e -> budgetRunOnMain { onMessage(e.message ?: "Não foi possível atualizar os orçamentos.") } }
        }.start()
    }

    Card(Modifier.fillMaxWidth().testTag("dashboard_budget")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Orçamentos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        if (currentBudgets.isEmpty()) {
            Text("Nenhum orçamento configurado para este mês.")
        } else {
            currentBudgets.forEach { budget ->
                BudgetProgressCard(
                    budget = budget,
                    categories = categories,
                    api = api,
                    onUpdated = { updated -> onBudgetsChanged(budgets.map { if (it.id == updated.id) updated else it }) },
                    onMessage = onMessage,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Text("Novo limite", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth().testTag("budget_amount"),
            label = { Text("Limite mensal (R$)") },
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth().testTag("budget_description"),
            label = { Text("Descrição") },
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { selectedCategoryId = null },
                label = { Text("Geral") },
            )
            androidx.compose.ui.layout.Layout(
                content = {
                    FilterChip(
                        selected = selectedCategoryId != null,
                        onClick = { categoryMenuOpen = true },
                        label = { Text(selectedCategoryName) },
                        modifier = Modifier.testTag("budget_category"),
                    )
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false },
                    ) {
                        categories.filter { it.type == "EXPENSE" || it.type == "BOTH" }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryMenuOpen = false
                                },
                            )
                        }
                    }
                },
                measurePolicy = { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
            )
        }

        val amountMinor = parseBudgetAmountMinor(amount)
        Button(
            enabled = amountMinor != null && amountMinor > 0,
            modifier = Modifier.testTag("budget_submit"),
            onClick = {
                val submitAmountMinor = amountMinor ?: return@Button
                Thread {
                    runCatching {
                        api.createBudget(
                            amountMinor = submitAmountMinor,
                            startOn = startOn,
                            endOn = endOn,
                            categoryId = selectedCategoryId,
                            description = description.ifBlank { null },
                        )
                    }.onSuccess {
                        val latest = runCatching { api.budgets() }.getOrDefault(budgets)
                        budgetRunOnMain {
                            onBudgetsChanged(latest)
                            amount = ""
                            description = ""
                            onMessage("Orçamento criado para " + month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))))
                        }
                    }.onFailure { e -> budgetRunOnMain { onMessage(e.message ?: "Erro ao criar orçamento.") } }
                }.start()
            },
        ) {
            Text("Salvar orçamento")
        }

        if (budgets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Histórico", style = MaterialTheme.typography.titleSmall)
            budgets.sortedByDescending { it.startOn }.take(8).forEach { budget ->
                val category = categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Geral"
                Text(
                    category + " · " + budget.startOn + " → " + budget.endOn +
                        " · " + budgetFormatMoney(budget.amountMinor) +
                        " · " + budget.status,
                    modifier = Modifier.testTag("budget_history_" + budget.id),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.TextButton(onClick = { refresh() }) { Text("Atualizar") }
        }
    }
}

@Composable
private fun BudgetProgressCard(
    budget: Budget,
    categories: List<Category>,
    api: ApiClient,
    onUpdated: (Budget) -> Unit,
    onMessage: (String) -> Unit,
) {
    val categoryName = categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Geral"
    Card(Modifier.fillMaxWidth().testTag("budget_" + budget.id)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(categoryName, style = MaterialTheme.typography.titleMedium)
                    Text(budget.description ?: "Orçamento mensal", style = MaterialTheme.typography.bodySmall)
                }
                Text(budget.alertLevel)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { budget.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().testTag("budget_progress"),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                budgetFormatMoney(budget.consumedMinor) + " consumidos de " + budgetFormatMoney(budget.amountMinor) +
                    " · " + budget.progressPercent + "%",
            )
            Text("Restante: " + budgetFormatMoney(budget.remainingMinor), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (budget.status == "ACTIVE") {
                    androidx.compose.material3.TextButton(onClick = {
                        Thread {
                            runCatching { api.pauseBudget(budget.id) }
                                .onSuccess { updated -> budgetRunOnMain { onUpdated(updated); onMessage("Orçamento pausado.") } }
                                .onFailure { e -> budgetRunOnMain { onMessage(e.message ?: "Erro ao pausar orçamento.") } }
                        }.start()
                    }) { Text("Pausar") }
                } else if (budget.status == "PAUSED") {
                    androidx.compose.material3.TextButton(onClick = {
                        Thread {
                            runCatching { api.resumeBudget(budget.id) }
                                .onSuccess { updated -> budgetRunOnMain { onUpdated(updated); onMessage("Orçamento retomado.") } }
                                .onFailure { e -> budgetRunOnMain { onMessage(e.message ?: "Erro ao retomar orçamento.") } }
                        }.start()
                    }) { Text("Retomar") }
                }
                if (budget.status != "CANCELLED") {
                    androidx.compose.material3.TextButton(onClick = {
                        Thread {
                            runCatching { api.cancelBudget(budget.id) }
                                .onSuccess { updated -> budgetRunOnMain { onUpdated(updated); onMessage("Orçamento cancelado.") } }
                                .onFailure { e -> budgetRunOnMain { onMessage(e.message ?: "Erro ao cancelar orçamento.") } }
                        }.start()
                    }) { Text("Cancelar") }
                }
            }
        }
    }
}


private fun parseBudgetAmountMinor(value: String): Long? {
    val raw = value.trim().removePrefix("R$").replace(" ", "")
    if (raw.isBlank()) return null
    val canonical = if (raw.contains(",")) {
        raw.replace(".", "").replace(",", ".")
    } else {
        raw
    }
    return runCatching {
        BigDecimal(canonical).movePointRight(2).longValueExact()
    }.getOrNull()
}

private fun budgetFormatMoney(minor: Long): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", minor / 100.0)

private fun budgetRunOnMain(action: () -> Unit) =
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
