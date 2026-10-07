package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val message: String,
    val readAt: String?,
    val createdAt: String,
)
