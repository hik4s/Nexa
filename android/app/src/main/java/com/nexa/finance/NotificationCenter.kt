package com.nexa.finance

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object NotificationCenter {
    private const val CHANNEL_ID = "nexa_finance_alerts"
    private const val PREFS = "nexa_notification_preferences"
    private const val ENABLED = "system_notifications_enabled"
    private const val PUBLISHED = "published_ids"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled)
            .apply()
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun publishUnread(context: Context, notifications: List<NexaNotification>) {
        if (!isEnabled(context) || !canPost(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val published = prefs.getStringSet(PUBLISHED, emptySet())?.toMutableSet() ?: mutableSetOf()
        notifications.filter { it.readAt == null && it.id !in published }.forEach { item ->
            val builder = if (Build.VERSION.SDK_INT >= 26) {
                android.app.Notification.Builder(context, CHANNEL_ID)
            } else {
                android.app.Notification.Builder(context)
            }
            builder
                .setSmallIcon(com.nexa.finance.R.drawable.ic_nexa)
                .setContentTitle(item.title)
                .setContentText(item.message)
                .setAutoCancel(true)
                .setPriority(
                    if (item.severity == "WARNING") android.app.Notification.PRIORITY_HIGH
                    else android.app.Notification.PRIORITY_DEFAULT
                )
            manager.notify(item.id.hashCode(), builder.build())
            published += item.id
        }
        prefs.edit().putStringSet(PUBLISHED, published.toList().takeLast(100).toSet()).apply()
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Alertas financeiros",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }
    }
}
