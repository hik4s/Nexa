package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateGoalRequest(
    val name: String,
    val targetAmountMinor: Long,
    val currency: String,
    val targetDate: String? = null,
    val description: String? = null,
)

@Serializable
data class CreateGoalContributionRequest(
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
)

@Serializable
data class GoalResponse(
    val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val remainingMinor: Long,
    val progressPercent: Int,
    val currency: String,
    val targetDate: String?,
    val description: String?,
    val status: String,
    val projectedDate: String?,
    val averageMonthlyContributionMinor: Long?,
    val projectionStatus: String,
)

@Serializable
data class GoalContributionResponse(
    val id: String,
    val goalId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val createdAt: String,
)

@Serializable
data class GoalDetailResponse(
    val goal: GoalResponse,
    val contributions: List<GoalContributionResponse>,
)
