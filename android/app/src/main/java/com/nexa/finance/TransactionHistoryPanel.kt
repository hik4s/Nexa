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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun TransactionHistoryPanel(
    transactions: List<Tx>,
    accounts: List<Account>,
    cards: List<Card>,
    categories: List<Category>,
) {
    var search by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("ALL") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var cardId by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf("ALL") }
    var sort by remember { mutableStateOf("DATE_DESC") }
    var categoryMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    var cardMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val startDate = when (period) {
        "THIS_MONTH" -> YearMonth.from(today).atDay(1)
        "LAST_30_DAYS" -> today.minusDays(29)
        "LAST_90_DAYS" -> today.minusDays(89)
        else -> null
    }

    val filtered = transactions
        .asSequence()
        .filter { tx ->
            val query = search.trim().lowercase(Locale.ROOT)
            query.isBlank() || tx.description.lowercase(Locale.ROOT).contains(query)
        }
        .filter { tx ->
            startDate == null || runCatching {
                val date = LocalDate.parse(tx.date)
                !date.isBefore(startDate) && !date.isAfter(today)
            }.getOrDefault(false)
        }
        .filter { categoryId == null || it.categoryId == categoryId }
        .filter { accountId == null || it.accountId == accountId }
        .filter { cardId == null || it.cardId == cardId }
        .filter { type == "ALL" || it.direction == type }
        .let { sequence ->
            when (sort) {
                "DATE_ASC" -> sequence.sortedBy { it.date }
                "AMOUNT_DESC" -> sequence.sortedByDescending { it.amountMinor }
                "AMOUNT_ASC" -> sequence.sortedBy { it.amountMinor }
                else -> sequence.sortedByDescending { it.date }
            }
        }
        .toList()

    val selectedCategory = categories.firstOrNull { it.id == categoryId }?.name ?: "Categoria"
    val selectedAccount = accounts.firstOrNull { it.id == accountId }?.name ?: "Conta"
    val selectedCard = cards.firstOrNull { it.id == cardId }?.name ?: "Cartão"
    val selectedSort = when (sort) {
        "DATE_ASC" -> "Mais antigas"
        "AMOUNT_DESC" -> "Maior valor"
        "AMOUNT_ASC" -> "Menor valor"
        else -> "Mais recentes"
    }

    Card(Modifier.fillMaxWidth().testTag("transaction_history")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Histórico", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().testTag("history_search"),
                label = { Text("Buscar descrição") },
            )

            Text("Período", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "Todos", "THIS_MONTH" to "Mês", "LAST_30_DAYS" to "30 dias", "LAST_90_DAYS" to "90 dias").forEach { (value, label) ->
                    FilterChip(
                        selected = period == value,
                        onClick = { period = value },
                        label = { Text(label) },
                        modifier = Modifier.testTag("history_period_$value"),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = type == "ALL", onClick = { type = "ALL" }, label = { Text("Todos") }, modifier = Modifier.testTag("history_type_all"))
                FilterChip(selected = type == "DEBIT", onClick = { type = "DEBIT" }, label = { Text("Despesas") }, modifier = Modifier.testTag("history_type_debit"))
                FilterChip(selected = type == "CREDIT", onClick = { type = "CREDIT" }, label = { Text("Entradas") }, modifier = Modifier.testTag("history_type_credit"))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { categoryMenu = true }, modifier = Modifier.testTag("history_category")) { Text(selectedCategory) }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        DropdownMenuItem(text = { Text("Todas") }, onClick = { categoryId = null; categoryMenu = false })
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = { categoryId = category.id; categoryMenu = false })
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { accountMenu = true }, modifier = Modifier.testTag("history_account")) { Text(selectedAccount) }
                    DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                        DropdownMenuItem(text = { Text("Todas") }, onClick = { accountId = null; accountMenu = false })
                        accounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.name) }, onClick = { accountId = account.id; accountMenu = false })
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { cardMenu = true }, modifier = Modifier.testTag("history_card")) { Text(selectedCard) }
                    DropdownMenu(expanded = cardMenu, onDismissRequest = { cardMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos") }, onClick = { cardId = null; cardMenu = false })
                        cards.forEach { card ->
                            DropdownMenuItem(text = { Text(card.name) }, onClick = { cardId = card.id; cardMenu = false })
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { sortMenu = true }, modifier = Modifier.testTag("history_sort")) { Text("Ordenar: $selectedSort") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("Mais recentes") }, onClick = { sort = "DATE_DESC"; sortMenu = false })
                    DropdownMenuItem(text = { Text("Mais antigas") }, onClick = { sort = "DATE_ASC"; sortMenu = false })
                    DropdownMenuItem(text = { Text("Maior valor") }, onClick = { sort = "AMOUNT_DESC"; sortMenu = false })
                    DropdownMenuItem(text = { Text("Menor valor") }, onClick = { sort = "AMOUNT_ASC"; sortMenu = false })
                }
                Text("${filtered.size} lançamento(s)", modifier = Modifier.testTag("history_count"))
            }

            if (filtered.isEmpty()) {
                Text("Nenhum lançamento encontrado.", modifier = Modifier.testTag("history_empty"))
            } else {
                filtered.take(50).forEach { tx ->
                    val expanded = expandedId == tx.id
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("history_tx_${tx.id}")) {
                        Column(Modifier.padding(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(tx.description, style = MaterialTheme.typography.titleSmall)
                                    Text(tx.date, style = MaterialTheme.typography.bodySmall)
                                }
                                Text((if (tx.direction == "DEBIT") "-" else "+") + formatHistoryMoney(tx.amountMinor))
                            }
                            TextButton(
                                onClick = { expandedId = if (expanded) null else tx.id },
                                modifier = Modifier.testTag("history_details_${tx.id}"),
                            ) { Text(if (expanded) "Ocultar detalhes" else "Detalhes") }
                            if (expanded) {
                                Text("Tipo: " + tx.direction)
                                Text("Conta: " + (accounts.firstOrNull { it.id == tx.accountId }?.name ?: "Não informada"))
                                Text("Categoria: " + (categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Sem categoria"))
                                Text("Cartão: " + (cards.firstOrNull { it.id == tx.cardId }?.name ?: "Não informado"))
                                tx.invoiceId?.let { Text("Fatura: $it") }
                                tx.installmentNumber?.let { Text("Parcela: $it") }
                                Text("Origem: " + (if (tx.cardId != null) "Cartão" else "Lançamento"))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    search = ""
                    period = "ALL"
                    categoryId = null
                    accountId = null
                    cardId = null
                    type = "ALL"
                    sort = "DATE_DESC"
                    expandedId = null
                },
                modifier = Modifier.testTag("history_clear_filters"),
            ) { Text("Limpar filtros") }
        }
    }
}

private fun formatHistoryMoney(minor: Long): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", minor / 100.0)
