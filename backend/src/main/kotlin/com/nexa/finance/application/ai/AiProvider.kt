package com.nexa.finance.application.ai

interface AiProvider {
    val name: String
    fun analyze(context: AiFinancialContext): AiAnalysis
    fun answer(context: AiFinancialContext, question: String): AiAnswer
}
