package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateInvoiceRequest(
    val cardId: String,
    val periodStart: String,
    val periodEnd: String,
    val dueOn: String,
)

@Serializable
data class InvoicePaymentRequest(
    val sourceAccountId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val source: String = "MANUAL",
)

@Serializable
data class InvoiceResponse(
    val id: String,
    val cardId: String,
    val periodStart: String,
    val periodEnd: String,
    val dueOn: String,
    val totalAmountMinor: Long,
    val paidAmountMinor: Long,
    val remainingAmountMinor: Long,
    val currency: String,
    val status: String,
    val transactionIds: List<String>,
    val paymentTransactionIds: List<String>,
)