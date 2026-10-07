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
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun GoalPanel(
    api: ApiClient,
    goals: List<Goal>,
    onGoalsChanged: (List<Goal>) -> Unit,
    onMessage: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(LocalDate.now().plusMonths(3).toString()) }
    var description by remember { mutableStateOf("") }
    var formMessage by remember { mutableStateOf("") }

    val visibleGoals = goals.filter { it.status != "CANCELLED" }

    Card(Modifier.fillMaxWidth().testTag("dashboard_goals")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Metas financeiras", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (visibleGoals.isEmpty()) {
                Text("Nenhuma meta configurada.")
            } else {
                visibleGoals.forEach { goal ->
                    GoalProgressCard(
                        goal = goal,
                        api = api,
                        onUpdated = { updated ->
                            onGoalsChanged(goals.map { if (it.id == updated.id) updated else it })
                        },
                        onMessage = onMessage,
                        onFormMessage = { formMessage = it },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Text("Nova meta", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().testTag("goal_name"),
                label = { Text("Nome da meta") },
            )
            OutlinedTextField(
                value = targetAmount,
                onValueChange = { targetAmount = it },
                modifier = Modifier.fillMaxWidth().testTag("goal_amount"),
                label = { Text("Valor-alvo (R$)") },
            )
            OutlinedTextField(
                value = targetDate,
                onValueChange = { targetDate = it },
                modifier = Modifier.fillMaxWidth().testTag("goal_target_date"),
                label = { Text("Prazo (AAAA-MM-DD, opcional)") },
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth().testTag("goal_description"),
                label = { Text("Descrição") },
            )

            val amountMinor = parseGoalAmountMinor(targetAmount)
            val parsedDate = targetDate.trim().takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
            val dateValid = targetDate.isBlank() || (parsedDate != null && !parsedDate.isBefore(LocalDate.now()))

            Button(
                enabled = name.isNotBlank() && amountMinor != null && amountMinor > 0 && dateValid,
                modifier = Modifier.testTag("goal_submit"),
                onClick = {
                    val submitAmountMinor = amountMinor ?: return@Button
                    val submitTargetDate = targetDate.trim().takeIf { it.isNotBlank() }
                    Thread {
                        runCatching {
                            api.createGoal(
                                name = name.trim(),
                                targetAmountMinor = submitAmountMinor,
                                targetDate = submitTargetDate,
                                description = description.ifBlank { null },
                            )
                        }.onSuccess {
                            val latest = runCatching { api.goals() }.getOrDefault(goals)
                            goalRunOnMain {
                                onGoalsChanged(latest)
                                name = ""
                                targetAmount = ""
                                description = ""
                                formMessage = "Meta criada."
                                onMessage("Meta criada.")
                            }
                        }.onFailure { e ->
                            goalRunOnMain {
                            formMessage = "Erro ao criar meta: " + (e.message ?: "erro desconhecido")
                            onMessage(e.message ?: "Erro ao criar meta.")
                        }
                        }
                    }.start()
                },
            ) {
                Text("Salvar meta")
            }
            if (formMessage.isNotBlank()) {
                Text(formMessage, modifier = Modifier.testTag("goal_form_message"))
            }

            if (goals.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Histórico", style = MaterialTheme.typography.titleSmall)
                goals.sortedByDescending { it.targetDate ?: "9999-12-31" }.take(8).forEach { goal ->
                    Text(
                        goal.name + " · " + goal.status + " · " +
                            goal.currentAmountMinor.formatGoalMoney() + " / " +
                            goal.targetAmountMinor.formatGoalMoney(),
                        modifier = Modifier.testTag("goal_history_" + goal.id),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(
    goal: Goal,
    api: ApiClient,
    onUpdated: (Goal) -> Unit,
    onMessage: (String) -> Unit,
    onFormMessage: (String) -> Unit,
) {
    var contributionAmount by remember(goal.id) { mutableStateOf("") }

    Card(Modifier.fillMaxWidth().testTag("goal_" + goal.id)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium)
                    goal.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                Text(goal.status)
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { goal.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().testTag("goal_progress_" + goal.id),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                goal.currentAmountMinor.formatGoalMoney() + " de " +
                    goal.targetAmountMinor.formatGoalMoney() + " · " + goal.progressPercent + "%",
            )
            Text("Falta: " + goal.remainingMinor.formatGoalMoney(), style = MaterialTheme.typography.bodySmall)
            Text(
                "Prazo: " + (goal.targetDate ?: "Sem prazo"),
                modifier = Modifier.testTag("goal_deadline_" + goal.id),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                goalProjectionText(goal),
                modifier = Modifier.testTag("goal_projection_" + goal.id),
                style = MaterialTheme.typography.bodySmall,
            )

            if (goal.status == "ACTIVE") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contributionAmount,
                    onValueChange = { contributionAmount = it },
                    modifier = Modifier.fillMaxWidth().testTag("goal_contribution_amount"),
                    label = { Text("Novo aporte (R$)") },
                )
                val contributionMinor = parseGoalAmountMinor(contributionAmount)
                Button(
                    enabled = contributionMinor != null && contributionMinor > 0,
                    modifier = Modifier.testTag("goal_contribute"),
                    onClick = {
                        val amount = contributionMinor ?: return@Button
                        Thread {
                            runCatching {
                                api.contributeGoal(goal.id, amount, LocalDate.now().toString())
                            }.onSuccess { updated ->
                                goalRunOnMain {
                                    onUpdated(updated)
                                    contributionAmount = ""
                                    val message = if (updated.status == "ACHIEVED") "Meta atingida." else "Aporte registrado."
                                    onFormMessage(message)
                                    onMessage(message)
                                }
                            }.onFailure { e ->
                                goalRunOnMain {
                                    val message = "Erro ao registrar aporte: " + (e.message ?: "erro desconhecido")
                                    onFormMessage(message)
                                    onMessage(e.message ?: "Erro ao registrar aporte.")
                                }
                            }
                        }.start()
                    },
                ) {
                    Text("Contribuir")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goal.status == "ACTIVE") {
                    androidx.compose.material3.TextButton(onClick = {
                        Thread {
                            runCatching { api.pauseGoal(goal.id) }
                                .onSuccess { updated -> goalRunOnMain { onUpdated(updated); onMessage("Meta pausada.") } }
                                .onFailure { e -> goalRunOnMain { onMessage(e.message ?: "Erro ao pausar meta.") } }
                        }.start()
                    }) { Text("Pausar") }
                } else if (goal.status == "PAUSED") {
                    androidx.compose.material3.TextButton(onClick = {
                        Thread {
                            runCatching { api.resumeGoal(goal.id) }
                                .onSuccess { updated -> goalRunOnMain { onUpdated(updated); onMessage("Meta retomada.") } }
                                .onFailure { e -> goalRunOnMain { onMessage(e.message ?: "Erro ao retomar meta.") } }
                        }.start()
                    }) { Text("Retomar") }
                }
                if (goal.status != "ACHIEVED") {
                    androidx.compose.material3.TextButton(onClick = {
                        Thread {
                            runCatching { api.cancelGoal(goal.id) }
                                .onSuccess { updated -> goalRunOnMain { onUpdated(updated); onMessage("Meta cancelada.") } }
                                .onFailure { e -> goalRunOnMain { onMessage(e.message ?: "Erro ao cancelar meta.") } }
                        }.start()
                    }) { Text("Cancelar") }
                }
            }
        }
    }
}

private fun parseGoalAmountMinor(value: String): Long? {
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

private fun goalProjectionText(goal: Goal): String {
    return when (goal.projectionStatus) {
        "ACHIEVED" -> "Projeção: meta atingida."
        "PAUSED" -> "Projeção: indisponível enquanto pausada."
        "NO_CONTRIBUTION" -> "Projeção: faça o primeiro aporte."
        "NO_DEADLINE" -> {
            val date = goal.projectedDate ?: "indisponível"
            "Projeção: " + formatGoalDate(date) + " · sem prazo definido"
        }
        "ON_TRACK" -> {
            val date = goal.projectedDate ?: "indisponível"
            val pace = goal.averageMonthlyContributionMinor?.formatGoalMoney()
            "Projeção: " + formatGoalDate(date) + " · no prazo" +
                (pace?.let { " · ritmo médio " + it + "/mês" } ?: "")
        }
        "AT_RISK" -> {
            val date = goal.projectedDate ?: "indisponível"
            val pace = goal.averageMonthlyContributionMinor?.formatGoalMoney()
            "Projeção: " + formatGoalDate(date) + " · após o prazo" +
                (pace?.let { " · ritmo médio " + it + "/mês" } ?: "")
        }
        else -> "Projeção: indisponível."
    }
}

private fun formatGoalDate(value: String): String =
    runCatching {
        LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrDefault(value)

private fun Long.formatGoalMoney(): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", this / 100.0)

private fun goalRunOnMain(action: () -> Unit) =
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
