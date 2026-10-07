package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateInstallmentRequest(
    val totalAmountMinor: Long,
    val currency: String,
    val count: Int,
    val firstDueOn: String,
    val description: String? = null,
    val categoryId: String? = null,
)

@Serializable
data class InstallmentItemResponse(
    val number: Int,
    val amountMinor: Long,
    val currency: String,
    val dueOn: String,
    val status: String,
)

@Serializable
data class InstallmentResponse(
    val id: String,
    val totalAmountMinor: Long,
    val currency: String,
    val count: Int,
    val firstDueOn: String,
    val description: String?,
    val categoryId: String?,
    val status: String,
    val items: List<InstallmentItemResponse>,
)
