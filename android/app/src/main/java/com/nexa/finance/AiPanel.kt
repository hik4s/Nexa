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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun AiPanel(
    analysis: AiAnalysis?,
    answer: AiAnswer?,
    onAsk: (String) -> Unit,
    loading: Boolean = false,
) {
    var question by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth().testTag("ai_panel")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NEXA Intelligence", style = MaterialTheme.typography.titleMedium)
                Text(analysis?.provider ?: "indisponível", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                analysis?.monthlySummary ?: "Análise financeira ainda não carregada.",
                modifier = Modifier.testTag("ai_summary"),
            )

            analysis?.insights?.forEach { insight ->
                Card(
                    Modifier.fillMaxWidth().padding(top = 8.dp).testTag("ai_insight_" + insight.id),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(insight.title, style = MaterialTheme.typography.titleSmall)
                        Text(insight.message)
                        if (insight.sourceFactIds.isNotEmpty()) {
                            Text(
                                "Fonte: " + insight.sourceFactIds.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(500) },
                modifier = Modifier.fillMaxWidth().testTag("ai_question"),
                label = { Text("Pergunte sobre suas finanças") },
                singleLine = true,
            )
            Button(
                onClick = {
                    val value = question.trim()
                    if (value.isNotEmpty()) {
                        onAsk(value)
                        question = ""
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().testTag("ai_ask"),
            ) {
                Text(if (loading) "Analisando..." else "Perguntar")
            }

            answer?.let {
                Spacer(Modifier.height(8.dp))
                Text(it.answer, modifier = Modifier.testTag("ai_answer"))
                if (it.sourceFactIds.isNotEmpty()) {
                    Text(
                        "Fontes: " + it.sourceFactIds.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
