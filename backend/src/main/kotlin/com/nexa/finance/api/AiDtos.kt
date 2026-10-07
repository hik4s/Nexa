package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class AiInsightResponse(
    val id: String,
    val level: String,
    val title: String,
    val message: String,
    val sourceFactIds: List<String>,
)

@Serializable
data class AiAnalysisResponse(
    val month: String,
    val provider: String,
    val monthlySummary: String,
    val contextFactCount: Int,
    val insights: List<AiInsightResponse>,
)

@Serializable
data class AiQuestionRequest(
    val question: String,
    val month: String? = null,
)

@Serializable
data class AiAnswerResponse(
    val answer: String,
    val sourceFactIds: List<String>,
)

@Serializable
data class AiUsageResponse(
    val usedToday: Int,
    val dailyLimit: Int,
)
