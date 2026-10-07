package com.nexa.finance

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable
fun NotificationPanel(
    notifications: List<NexaNotification>,
    onRead: (String) -> Unit,
) {
    val context = LocalContext.current
    var unreadOnly by remember { mutableStateOf(false) }
    var systemEnabled by remember { mutableStateOf(NotificationCenter.isEnabled(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemEnabled = granted
        NotificationCenter.setEnabled(context, granted)
        if (granted) NotificationCenter.publishUnread(context, notifications)
    }

    LaunchedEffect(notifications) {
        NotificationCenter.publishUnread(context, notifications)
    }

    val visible = if (unreadOnly) notifications.filter { it.readAt == null } else notifications

    Card(Modifier.fillMaxWidth().testTag("notification_panel")) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Alertas", style = MaterialTheme.typography.titleMedium)
                Text(
                    notifications.count { it.readAt == null }.toString() + " não lidos",
                    modifier = Modifier.testTag("notification_unread_count"),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !unreadOnly,
                    onClick = { unreadOnly = false },
                    label = { Text("Todos") },
                    modifier = Modifier.testTag("notifications_all"),
                )
                FilterChip(
                    selected = unreadOnly,
                    onClick = { unreadOnly = true },
                    label = { Text("Não lidos") },
                    modifier = Modifier.testTag("notifications_unread"),
                )
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        systemEnabled = true
                        NotificationCenter.setEnabled(context, true)
                        NotificationCenter.publishUnread(context, notifications)
                    }
                },
                enabled = !systemEnabled,
                modifier = Modifier.fillMaxWidth().testTag("notifications_enable_system"),
            ) {
                Text(if (systemEnabled) "Notificações do sistema ativadas" else "Ativar notificações do sistema")
            }

            if (visible.isEmpty()) {
                Text("Nenhum alerta no momento.", modifier = Modifier.testTag("notifications_empty"))
            } else {
                visible.take(20).forEach { notification ->
                    Card(
                        Modifier.fillMaxWidth().padding(top = 8.dp).testTag("notification_" + notification.id),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(notification.title, style = MaterialTheme.typography.titleSmall)
                            Text(notification.message)
                            Text(notification.createdAt, style = MaterialTheme.typography.labelSmall)
                            if (notification.readAt == null) {
                                Button(
                                    onClick = { onRead(notification.id) },
                                    modifier = Modifier.testTag("notification_read_" + notification.id),
                                ) { Text("Marcar como lido") }
                            }
                        }
                    }
                }
            }
        }
    }
}
