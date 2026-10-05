package com.nexa.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexa.finance.sync.LocalDatabaseProvider
import com.nexa.finance.sync.SessionStore
import com.nexa.finance.sync.SyncConflictResolver

@Composable
fun SyncConflictPanel(
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val userId = remember { SessionStore(context).userId() }
    var conflicts by remember { mutableStateOf(emptyList<com.nexa.finance.sync.SyncConflictSummary>()) }
    var busyOperationId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        val currentUserId = userId ?: return
        Thread {
            runCatching {
                val database = LocalDatabaseProvider.get(context)
                SyncConflictResolver(
                    context = context,
                    conflictDao = database.syncConflictDao(),
                    operationDao = database.syncOperationDao(),
                    transactionDao = database.localTransactionDao(),
                ).pending(currentUserId)
            }.onSuccess { result ->
                runOnMain { conflicts = result }
            }.onFailure { error ->
                runOnMain { onMessage(error.message ?: "Não foi possível carregar conflitos de sincronização.") }
            }
        }.start()
    }

    fun resolve(operationId: String, keepServer: Boolean) {
        if (busyOperationId != null) return
        busyOperationId = operationId
        Thread {
            val result = runCatching {
                val database = LocalDatabaseProvider.get(context)
                val resolver = SyncConflictResolver(
                    context = context,
                    conflictDao = database.syncConflictDao(),
                    operationDao = database.syncOperationDao(),
                    transactionDao = database.localTransactionDao(),
                )
                if (keepServer) resolver.keepServer(operationId) else resolver.resubmitMine(operationId)
            }
            runOnMain {
                busyOperationId = null
                result.onSuccess {
                    conflicts = conflicts.filterNot { it.operationId == operationId }
                    onMessage(
                        if (keepServer) {
                            "Conflito resolvido: estado do servidor mantido."
                        } else {
                            "Seu lançamento foi reenviado com a versão atual do servidor."
                        }
                    )
                    if (keepServer) refresh()
                }.onFailure { error ->
                    onMessage(error.message ?: "Não foi possível resolver o conflito.")
                    refresh()
                }
            }
        }.start()
    }

    LaunchedEffect(userId) { refresh() }

    if (conflicts.isNotEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("sync_conflicts_panel"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Conflitos de sincronização", style = MaterialTheme.typography.titleLarge)
            Text(
                "Um lançamento offline foi diferente do estado salvo no servidor. Escolha qual versão deve prevalecer.",
                style = MaterialTheme.typography.bodySmall,
            )

            conflicts.forEach { conflict ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .testTag("sync_conflict_${conflict.operationId}"),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Lançamento em conflito",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Seu lançamento: " +
                                (conflict.localAmountMinor?.let(::formatMoney) ?: "valor indisponível") +
                                " · " + conflict.localDescription,
                        )
                        Text(
                            "Servidor: " +
                                (conflict.serverAmountMinor?.let(::formatMoney) ?: "snapshot indisponível") +
                                " · " + (conflict.serverDescription ?: "estado atual"),
                        )
                        conflict.serverVersion?.let {
                            Text(
                                "Versão do servidor: $it",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val busy = busyOperationId == conflict.operationId
                            OutlinedButton(
                                onClick = { resolve(conflict.operationId, keepServer = true) },
                                enabled = !busy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_conflict_keep_server"),
                            ) {
                                Text("Manter servidor")
                            }
                            Button(
                                onClick = { resolve(conflict.operationId, keepServer = false) },
                                enabled = !busy && conflict.serverVersion != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_conflict_resubmit"),
                            ) {
                                Text(if (busy) "Processando..." else "Usar meu lançamento")
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = ::refresh,
                modifier = Modifier.testTag("sync_conflicts_refresh"),
            ) {
                Text("Atualizar conflitos")
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
