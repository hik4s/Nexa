package com.nexa.finance

import com.nexa.finance.api.ApiError
import com.nexa.finance.api.accountRoutes
import com.nexa.finance.api.aiRoutes
import com.nexa.finance.api.authRoutes
import com.nexa.finance.api.budgetRoutes
import com.nexa.finance.api.categoryRoutes
import com.nexa.finance.api.cardRoutes
import com.nexa.finance.api.dataExportRoutes
import com.nexa.finance.api.goalRoutes
import com.nexa.finance.api.installmentRoutes
import com.nexa.finance.api.invoiceRoutes
import com.nexa.finance.api.notificationRoutes
import com.nexa.finance.api.recurrenceRoutes
import com.nexa.finance.api.refundRoutes
import com.nexa.finance.api.transactionRoutes
import com.nexa.finance.api.syncRoutes
import com.nexa.finance.api.transferRoutes
import com.nexa.finance.application.account.AccountService
import com.nexa.finance.application.ai.AiContextBuilder
import com.nexa.finance.application.ai.AiService
import com.nexa.finance.application.ai.RuleBasedAiProvider
import com.nexa.finance.application.auth.AuthenticationService
import com.nexa.finance.application.auth.ProfileService
import com.nexa.finance.application.auth.RecoveryService
import com.nexa.finance.application.budget.BudgetService
import com.nexa.finance.application.card.CardService
import com.nexa.finance.application.export.DataExportService
import com.nexa.finance.application.goal.GoalService
import com.nexa.finance.application.installment.InstallmentService
import com.nexa.finance.application.invoice.InvoiceService
import com.nexa.finance.application.notification.NotificationService
import com.nexa.finance.application.recurrence.RecurrenceService
import com.nexa.finance.application.refund.RefundService
import com.nexa.finance.application.transaction.TransactionService
import com.nexa.finance.application.sync.SyncService
import com.nexa.finance.application.transfer.TransferService
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.ai.JdbcAiUsageRepository
import com.nexa.finance.infrastructure.budget.JdbcBudgetRepository
import com.nexa.finance.infrastructure.card.JdbcCardRepository
import com.nexa.finance.infrastructure.category.JdbcCategoryRepository
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import com.nexa.finance.infrastructure.goal.JdbcGoalRepository
import com.nexa.finance.infrastructure.export.JdbcDataExportRepository
import com.nexa.finance.infrastructure.installment.JdbcInstallmentRepository
import com.nexa.finance.infrastructure.invoice.JdbcInvoiceRepository
import com.nexa.finance.infrastructure.notification.JdbcNotificationRepository
import com.nexa.finance.infrastructure.recurrence.JdbcRecurrenceRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import com.nexa.finance.infrastructure.sync.JdbcSyncChangeRepository
import com.nexa.finance.infrastructure.transfer.JdbcTransferRepository
import com.nexa.finance.infrastructure.user.JdbcUserAuthRepository
import io.ktor.serialization.JsonConvertException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import kotlin.math.ceil
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val settings = ServerSettings.fromEnvironment()
    embeddedServer(Netty, configure = {
        connectors.add(EngineConnectorBuilder().apply {
            host = settings.host
            port = settings.port
        })
        val timeoutSeconds = ceil(settings.requestTimeoutMillis / 1_000.0).toInt()
        requestReadTimeoutSeconds = timeoutSeconds
        responseWriteTimeoutSeconds = timeoutSeconds
    }) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val settings = ServerSettings.fromEnvironment()
    val rateLimitSettings = RateLimitSettings.fromEnvironment()
    val database = DatabaseConnectionProvider()

    val userRepository = JdbcUserAuthRepository(database)
    val accountRepository = JdbcAccountRepository(database)
    val categoryRepository = JdbcCategoryRepository(database)
    val transactionRepository = JdbcTransactionRepository(database)
    val transferRepository = JdbcTransferRepository(database)
    val cardRepository = JdbcCardRepository(database)
    val invoiceRepository = JdbcInvoiceRepository(database)
    val installmentRepository = JdbcInstallmentRepository(database)
    val recurrenceRepository = JdbcRecurrenceRepository(database)
    val budgetRepository = JdbcBudgetRepository(database)
    val goalRepository = JdbcGoalRepository(database)
    val aiUsageRepository = JdbcAiUsageRepository(database)
    val notificationRepository = JdbcNotificationRepository(database)
    val dataExport = DataExportService(JdbcDataExportRepository(database))

    val authentication = AuthenticationService(userRepository)
    val recovery = RecoveryService(userRepository)
    val profile = ProfileService(userRepository)
    val accounts = AccountService(accountRepository)
    val transactions = TransactionService(transactionRepository, accountRepository)
    val syncChanges = JdbcSyncChangeRepository(database)
    val sync = SyncService(transactions, transactionRepository, syncChanges)
    val transfers = TransferService(transferRepository, accountRepository)
    val cards = CardService(cardRepository, accountRepository, transactionRepository, installmentRepository)
    val invoices = InvoiceService(invoiceRepository, cardRepository, accountRepository)
    val installments = InstallmentService(installmentRepository, transactionRepository)
    val refunds = RefundService(transactionRepository, cardRepository)
    val recurrences = RecurrenceService(recurrenceRepository, accountRepository, transactionRepository)
    val budgets = BudgetService(budgetRepository)
    val goals = GoalService(goalRepository)
    val aiContext = AiContextBuilder(transactionRepository, budgetRepository, goalRepository, categoryRepository)
    val ai = AiService(aiContext, RuleBasedAiProvider(), aiUsageRepository)
    val notifications = NotificationService(
        repository = notificationRepository,
        budgets = budgets,
        goals = goals,
        recurrences = recurrences,
        invoices = invoiceRepository,
        aiContext = aiContext,
    )

    monitor.subscribe(ApplicationStopped) {
        database.close()
    }

    install(CallLogging) {
        filter { call -> call.request.path().startsWith("/api/v1") }
    }

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<DomainValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", cause.message ?: "Invalid request."))
        }
        status(HttpStatusCode.TooManyRequests) { call, _ ->
            call.respond(HttpStatusCode.TooManyRequests, ApiError("RATE_LIMITED", "Too many requests. Please try again later."))
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

    install(XForwardedHeaders)

    install(RateLimit) {
        register(RateLimitName("api")) {
            rateLimiter(limit = rateLimitSettings.requestsPerMinute, refillPeriod = rateLimitSettings.refillPeriod)
            requestKey { call ->
                call.request.origin.remoteHost
            }
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
        rateLimit(RateLimitName("api")) {
            authRoutes(authentication, recovery, profile)
            dataExportRoutes(dataExport)
            accountRoutes(accounts)
        categoryRoutes(categoryRepository)
        transactionRoutes(transactions)
        syncRoutes(sync)
        transferRoutes(transfers)
        cardRoutes(cards)
        invoiceRoutes(invoices)
        installmentRoutes(installments)
        refundRoutes(refunds)
        recurrenceRoutes(recurrences)
        budgetRoutes(budgets)
        goalRoutes(goals)
        aiRoutes(ai)
            notificationRoutes(notifications)
        }
    }
}
