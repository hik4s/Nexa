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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@Composable
fun RecurrencePanel(
    api: ApiClient,
    recurrences: List<Recurrence>,
    candidates: List<RecurrenceCandidate>,
    accounts: List<Account>,
    categories: List<Category>,
    onRecurrencesChanged: (List<Recurrence>) -> Unit,
    onCandidatesChanged: (List<RecurrenceCandidate>) -> Unit,
    onMessage: (String) -> Unit,
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull { it.type != "CREDIT_CARD" }?.id.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var interval by remember { mutableStateOf("1") }
    var startOn by remember { mutableStateOf(LocalDate.now().toString()) }
    var endOn by remember { mutableStateOf("") }
    var maxOccurrences by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    fun loadIntoForm(recurrence: Recurrence) {
        editingId = recurrence.id
        selectedAccountId = recurrence.accountId
        amount = (recurrence.amountMinor / 100.0).let { String.format(Locale.US, "%.2f", it) }
        transactionType = recurrence.transactionType
        frequency = recurrence.frequency
        interval = recurrence.interval.toString()
        startOn = recurrence.startOn
        endOn = recurrence.endOn.orEmpty()
        maxOccurrences = recurrence.maxOccurrences?.toString().orEmpty()
        description = recurrence.description
        categoryId = recurrence.categoryId
    }

    fun clearForm() {
        editingId = null
        amount = ""
        transactionType = "EXPENSE"
        frequency = "MONTHLY"
        interval = "1"
        startOn = LocalDate.now().toString()
        endOn = ""
        maxOccurrences = ""
        description = ""
        categoryId = null
    }

    fun refresh() {
        Thread {
            runCatching { api.recurrences() }
                .onSuccess { latest -> recurrenceRunOnMain { onRecurrencesChanged(latest) } }
                .onFailure { e -> recurrenceRunOnMain { onMessage(e.message ?: "Não foi possível atualizar as recorrências.") } }
            runCatching { api.recurrenceCandidates() }
                .onSuccess { latest -> recurrenceRunOnMain { onCandidatesChanged(latest) } }
        }.start()
    }

    val availableAccounts = accounts.filter { it.type != "CREDIT_CARD" }
    val selectedAccountName = availableAccounts.firstOrNull { it.id == selectedAccountId }?.name ?: "Selecione a conta"
    val selectedCategoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "Sem categoria"
    val amountMinor = parseRecurrenceAmountMinor(amount)
    val parsedInterval = interval.toIntOrNull()
    val parsedMax = maxOccurrences.toIntOrNull()
    val dateValid = runCatching {
        val start = LocalDate.parse(startOn)
        val end = endOn.trim().takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        end == null || !end.isBefore(start)
    }.getOrDefault(false)
    val canSave = selectedAccountId.isNotBlank() &&
        amountMinor != null && amountMinor > 0 &&
        parsedInterval != null && parsedInterval > 0 &&
        dateValid &&
        description.isNotBlank()

    Card(Modifier.fillMaxWidth().testTag("dashboard_recurring")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                if (editingId == null) "Recorrências" else "Editar recorrência",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            if (availableAccounts.isEmpty()) {
                Text("Cadastre uma conta para configurar recorrências.")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = transactionType == "EXPENSE",
                        onClick = { transactionType = "EXPENSE" },
                        label = { Text("Despesa") },
                    )
                    FilterChip(
                        selected = transactionType == "INCOME",
                        onClick = { transactionType = "INCOME" },
                        label = { Text("Entrada") },
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth().testTag("recurrence_amount"),
                    label = { Text("Valor (R$)") },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().testTag("recurrence_description"),
                    label = { Text("Descrição") },
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        TextButton(onClick = { accountMenuOpen = true }, modifier = Modifier.testTag("recurrence_account")) {
                            Text(selectedAccountName)
                        }
                        DropdownMenu(expanded = accountMenuOpen, onDismissRequest = { accountMenuOpen = false }) {
                            availableAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        selectedAccountId = account.id
                                        accountMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        TextButton(onClick = { categoryMenuOpen = true }, modifier = Modifier.testTag("recurrence_category")) {
                            Text(selectedCategoryName)
                        }
                        DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Sem categoria") },
                                onClick = {
                                    categoryId = null
                                    categoryMenuOpen = false
                                },
                            )
                            categories.filter { it.type == "EXPENSE" || it.type == "BOTH" }.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categoryId = category.id
                                        categoryMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { option ->
                        FilterChip(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            label = { Text(option.take(3)) },
                            modifier = Modifier.testTag("recurrence_frequency_$option"),
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it },
                        modifier = Modifier.weight(1f).testTag("recurrence_interval"),
                        label = { Text("Intervalo") },
                    )
                    OutlinedTextField(
                        value = maxOccurrences,
                        onValueChange = { maxOccurrences = it },
                        modifier = Modifier.weight(1f).testTag("recurrence_max_occurrences"),
                        label = { Text("Máx. ocorrências") },
                    )
                }

                OutlinedTextField(
                    value = startOn,
                    onValueChange = { startOn = it },
                    modifier = Modifier.fillMaxWidth().testTag("recurrence_start"),
                    label = { Text("Início (AAAA-MM-DD)") },
                )
                OutlinedTextField(
                    value = endOn,
                    onValueChange = { endOn = it },
                    modifier = Modifier.fillMaxWidth().testTag("recurrence_end"),
                    label = { Text("Fim (opcional)") },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canSave,
                        modifier = Modifier.testTag("recurrence_submit"),
                        onClick = {
                            val submitAmount = amountMinor ?: return@Button
                            val submitInterval = parsedInterval ?: return@Button
                            Thread {
                                val operation = runCatching {
                                    if (editingId == null) {
                                        api.createRecurrence(
                                            accountId = selectedAccountId,
                                            amountMinor = submitAmount,
                                            transactionType = transactionType,
                                            frequency = frequency,
                                            interval = submitInterval,
                                            startOn = startOn,
                                            endOn = endOn.trim().takeIf { it.isNotBlank() },
                                            maxOccurrences = parsedMax,
                                            description = description.trim(),
                                            categoryId = categoryId,
                                        )
                                    } else {
                                        api.updateRecurrence(
                                            id = editingId!!,
                                            accountId = selectedAccountId,
                                            amountMinor = submitAmount,
                                            transactionType = transactionType,
                                            frequency = frequency,
                                            interval = submitInterval,
                                            startOn = startOn,
                                            endOn = endOn.trim().takeIf { it.isNotBlank() },
                                            maxOccurrences = parsedMax,
                                            description = description.trim(),
                                            categoryId = categoryId,
                                        )
                                    }
                                }
                                operation.onSuccess {
                                    val latest = runCatching { api.recurrences() }.getOrDefault(recurrences)
                                    recurrenceRunOnMain {
                                        onRecurrencesChanged(latest)
                                        clearForm()
                                        onMessage("Recorrência salva.")
                                    }
                                }.onFailure { e ->
                                    recurrenceRunOnMain { onMessage(e.message ?: "Erro ao salvar recorrência.") }
                                }
                            }.start()
                        },
                    ) {
                        Text(if (editingId == null) "Criar" else "Salvar")
                    }
                    TextButton(onClick = { clearForm() }) {
                        Text("Limpar")
                    }
                    TextButton(onClick = { refresh() }) {
                        Text("Atualizar")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (candidates.isNotEmpty()) {
                Text("Possíveis recorrências", style = MaterialTheme.typography.titleSmall)
                candidates.take(5).forEachIndexed { index, candidate ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("recurrence_candidate_$index")
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(candidate.description, style = MaterialTheme.typography.titleSmall)
                            Text(
                                candidate.frequency.lowercase(Locale.ROOT) +
                                    " · " + candidate.occurrenceCount + " ocorrências · " +
                                    candidate.confidencePercent + "% de confiança",
                                modifier = Modifier.testTag("recurrence_candidate_confidence_" + index),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                formatRecurrenceMoney(candidate.amountMinor) +
                                    " · próxima: " + candidate.nextOccurrenceOn,
                            )
                            TextButton(onClick = {
                                selectedAccountId = candidate.accountId
                                amount = String.format(Locale.US, "%.2f", candidate.amountMinor / 100.0)
                                transactionType = candidate.transactionType
                                frequency = candidate.frequency
                                interval = "1"
                                categoryId = candidate.categoryId
                                description = candidate.description
                                startOn = candidate.nextOccurrenceOn
                                editingId = null
                                onMessage("Sugestão carregada no formulário.")
                            }) {
                                Text("Usar sugestão")
                            }
                        }
                    }
                }
            }

            Text("Agendamentos", style = MaterialTheme.typography.titleSmall)
            if (recurrences.isEmpty()) {
                Text("Nenhuma recorrência configurada.")
            } else {
                recurrences.forEach { recurrence ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("recurrence_" + recurrence.id)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(recurrence.description, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        recurrence.frequency.lowercase(Locale.ROOT) +
                                            " · " + formatRecurrenceMoney(recurrence.amountMinor),
                                    )
                                }
                                Text(recurrence.alertLevel, modifier = Modifier.testTag("recurrence_alert_" + recurrence.id))
                            }
                            Text("Próxima: " + (recurrence.nextOccurrenceOn ?: "sem ocorrência"))
                            if (recurrence.nextOccurrences.isNotEmpty()) {
                                Text(
                                    "Próximas: " + recurrence.nextOccurrences.joinToString(" · ") { raw ->
                                        raw.substringAfter("|")
                                    },
                                    modifier = Modifier.testTag("recurrence_next_" + recurrence.id),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { loadIntoForm(recurrence) },
                                    modifier = Modifier.testTag("recurrence_edit_" + recurrence.id),
                                ) { Text("Editar") }

                                if (recurrence.status == "ACTIVE") {
                                    TextButton(
                                        onClick = {
                                            Thread {
                                                runCatching { api.pauseRecurrence(recurrence.id) }
                                                    .onSuccess { updated ->
                                                        recurrenceRunOnMain {
                                                            onRecurrencesChanged(recurrences.map { if (it.id == updated.id) updated else it })
                                                            onMessage("Recorrência pausada.")
                                                        }
                                                    }
                                                    .onFailure { e -> recurrenceRunOnMain { onMessage(e.message ?: "Erro ao pausar.") } }
                                            }.start()
                                        },
                                        modifier = Modifier.testTag("recurrence_pause_" + recurrence.id),
                                    ) { Text("Pausar") }
                                } else if (recurrence.status == "PAUSED") {
                                    TextButton(
                                        onClick = {
                                            Thread {
                                                runCatching { api.resumeRecurrence(recurrence.id) }
                                                    .onSuccess { updated ->
                                                        recurrenceRunOnMain {
                                                            onRecurrencesChanged(recurrences.map { if (it.id == updated.id) updated else it })
                                                            onMessage("Recorrência retomada.")
                                                        }
                                                    }
                                                    .onFailure { e -> recurrenceRunOnMain { onMessage(e.message ?: "Erro ao retomar.") } }
                                            }.start()
                                        },
                                    ) { Text("Retomar") }
                                }

                                if (recurrence.status != "CANCELLED") {
                                    TextButton(
                                        onClick = {
                                            Thread {
                                                runCatching { api.cancelRecurrence(recurrence.id) }
                                                    .onSuccess { updated ->
                                                        recurrenceRunOnMain {
                                                            onRecurrencesChanged(recurrences.map { if (it.id == updated.id) updated else it })
                                                            onMessage("Recorrência encerrada.")
                                                        }
                                                    }
                                                    .onFailure { e -> recurrenceRunOnMain { onMessage(e.message ?: "Erro ao encerrar.") } }
                                            }.start()
                                        },
                                        modifier = Modifier.testTag("recurrence_cancel_" + recurrence.id),
                                    ) { Text("Encerrar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseRecurrenceAmountMinor(value: String): Long? {
    val raw = value.trim().removePrefix("R$").replace(" ", "")
    if (raw.isBlank()) return null
    val canonical = if (raw.contains(",")) raw.replace(".", "").replace(",", ".") else raw
    return runCatching { BigDecimal(canonical).movePointRight(2).longValueExact() }.getOrNull()
}

private fun formatRecurrenceMoney(minor: Long): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", minor / 100.0)

private fun recurrenceRunOnMain(action: () -> Unit) =
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
