package com.nexa.finance

import io.ktor.serialization.kotlinx.json.json
import com.nexa.finance.api.ApiError
import com.nexa.finance.api.accountRoutes
import com.nexa.finance.api.transactionRoutes
import com.nexa.finance.api.transferRoutes
import com.nexa.finance.application.transaction.TransactionService
import com.nexa.finance.application.transfer.TransferService
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import com.nexa.finance.infrastructure.transfer.JdbcTransferRepository
import com.nexa.finance.domain.common.DomainValidationException
import io.ktor.http.HttpStatusCode
import com.nexa.finance.api.authRoutes
import com.nexa.finance.application.account.AccountService
import com.nexa.finance.application.auth.AuthenticationService
import com.nexa.finance.application.auth.ProfileService
import com.nexa.finance.application.auth.RecoveryService
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.user.JdbcUserAuthRepository
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
    val accounts = AccountService(accountRepository)
    val transactions = TransactionService(JdbcTransactionRepository(), accountRepository)
    val transfers = TransferService(JdbcTransferRepository(), accountRepository)

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
            call.respondText("NEXA Finance API — OK")
        }
        authRoutes(authentication, recovery, profile)
        accountRoutes(accounts)
        transactionRoutes(transactions)
        transferRoutes(transfers)
    }
}
