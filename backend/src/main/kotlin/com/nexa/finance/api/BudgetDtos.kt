package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateBudgetRequest(
    val amountMinor: Long,
    val currency: String,
    val startOn: String,
    val endOn: String,
    val categoryId: String? = null,
    val description: String? = null,
)

@Serializable
data class UpdateBudgetRequest(
    val amountMinor: Long,
    val currency: String,
    val startOn: String,
    val endOn: String,
    val categoryId: String? = null,
    val description: String? = null,
)

@Serializable
data class BudgetResponse(
    val id: String,
    val amountMinor: Long,
    val consumedMinor: Long,
    val remainingMinor: Long,
    val progressPercent: Int,
    val alertLevel: String,
    val currency: String,
    val startOn: String,
    val endOn: String,
    val categoryId: String?,
    val description: String?,
    val status: String,
)

@Serializable
data class BudgetStatusResponse(
    val budget: BudgetResponse,
)
