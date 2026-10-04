package com.nexa.finance

import io.ktor.serialization.kotlinx.json.json
import com.nexa.finance.api.ApiError
import com.nexa.finance.api.accountRoutes
import com.nexa.finance.api.categoryRoutes
import com.nexa.finance.api.cardRoutes
import com.nexa.finance.api.invoiceRoutes
import com.nexa.finance.api.installmentRoutes
import com.nexa.finance.api.refundRoutes
import com.nexa.finance.api.recurrenceRoutes
import com.nexa.finance.api.transactionRoutes
import com.nexa.finance.api.transferRoutes
import com.nexa.finance.application.transaction.TransactionService
import com.nexa.finance.application.transfer.TransferService
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import com.nexa.finance.infrastructure.transfer.JdbcTransferRepository
import com.nexa.finance.domain.common.DomainValidationException
import io.ktor.http.HttpStatusCode
import com.nexa.finance.api.authRoutes
import com.nexa.finance.api.budgetRoutes
import com.nexa.finance.api.goalRoutes
import com.nexa.finance.api.aiRoutes
import com.nexa.finance.application.account.AccountService
import com.nexa.finance.application.budget.BudgetService
import com.nexa.finance.application.goal.GoalService
import com.nexa.finance.application.ai.AiContextBuilder
import com.nexa.finance.application.ai.AiService
import com.nexa.finance.application.ai.RuleBasedAiProvider
import com.nexa.finance.infrastructure.ai.JdbcAiUsageRepository
import com.nexa.finance.application.auth.AuthenticationService
import com.nexa.finance.application.auth.ProfileService
import com.nexa.finance.application.auth.RecoveryService
import com.nexa.finance.application.card.CardService
import com.nexa.finance.application.invoice.InvoiceService
import com.nexa.finance.application.installment.InstallmentService
import com.nexa.finance.infrastructure.installment.JdbcInstallmentRepository
import com.nexa.finance.application.refund.RefundService
import com.nexa.finance.application.recurrence.RecurrenceService
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.budget.JdbcBudgetRepository
import com.nexa.finance.infrastructure.goal.JdbcGoalRepository
import com.nexa.finance.infrastructure.category.JdbcCategoryRepository
import com.nexa.finance.infrastructure.card.JdbcCardRepository
import com.nexa.finance.infrastructure.invoice.JdbcInvoiceRepository
import com.nexa.finance.infrastructure.user.JdbcUserAuthRepository
import com.nexa.finance.infrastructure.recurrence.JdbcRecurrenceRepository
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.ContentTransformationException
import io.ktor.serialization.JsonConvertException
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val repository = JdbcUserAuthRepository()
    val authentication = AuthenticationService(repository)
    val recovery = RecoveryService(repository)
    val profile = ProfileService(repository)
    val accountRepository = JdbcAccountRepository()
    val categories = JdbcCategoryRepository()
    val accounts = AccountService(accountRepository)
    val transactions = TransactionService(JdbcTransactionRepository(), accountRepository)
    val transfers = TransferService(JdbcTransferRepository(), accountRepository)
    val cardRepository = JdbcCardRepository()
    val cards = CardService(cardRepository, accountRepository, JdbcTransactionRepository(), JdbcInstallmentRepository())
    val invoices = InvoiceService(JdbcInvoiceRepository(), cardRepository, accountRepository)
    val installments = InstallmentService(JdbcInstallmentRepository(), JdbcTransactionRepository())
    val refunds = RefundService(JdbcTransactionRepository(), cardRepository)
    val recurrences = RecurrenceService(JdbcRecurrenceRepository(), accountRepository, JdbcTransactionRepository())
    val budgets = BudgetService(JdbcBudgetRepository())
    val goals = GoalService(JdbcGoalRepository())
    val aiContext = AiContextBuilder(JdbcTransactionRepository(), JdbcBudgetRepository(), JdbcGoalRepository(), categories)
    val ai = AiService(aiContext, RuleBasedAiProvider(), JdbcAiUsageRepository())

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<DomainValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", cause.message ?: "Invalid request."))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_BODY", "Request body is invalid."))
        }
        exception<JsonConvertException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_BODY", "Request body is invalid."))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ApiError("INTERNAL_ERROR", "An unexpected error occurred."))
        }
    }

    install(Authentication) {
        bearer("session-bearer") {
            realm = "NEXA Finance API"
            authenticate { credential ->
                runCatching {
                    val userId = authentication.authenticate(credential.token)
                    UserIdPrincipal(userId.value.toString())
                }.getOrNull()
            }
        }
    }

    routing {
        get("/health") {
            call.respondText("NEXA Finance API - OK")
        }
        authRoutes(authentication, recovery, profile)
        accountRoutes(accounts)
        categoryRoutes(categories)
        transactionRoutes(transactions)
        transferRoutes(transfers)
        cardRoutes(cards)
        invoiceRoutes(invoices)
        installmentRoutes(installments)
        refundRoutes(refunds)
        recurrenceRoutes(recurrences)
        budgetRoutes(budgets)
        goalRoutes(goals)
        aiRoutes(ai)
    }
}
