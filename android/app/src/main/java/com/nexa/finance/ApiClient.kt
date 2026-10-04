package com.nexa.finance

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private val baseUrl: String = BuildConfig.NEXA_API_URL) {
    var token: String? = null

    fun withToken(value: String): ApiClient {
        token = value
        return this
    }

    fun login(email: String, password: String): String {
        val body = request("POST", "/api/v1/auth/login", JSONObject().apply {
            put("email", email)
            put("password", password)
        })
        token = body.getString("token")
        return body.getString("userId")
    }

    fun register(email: String, name: String, password: String) {
        request("POST", "/api/v1/auth/register", JSONObject().apply {
            put("email", email)
            put("displayName", name)
            put("password", password)
            put("timeZone", "America/Sao_Paulo")
        })
    }

    fun accounts(): List<Account> {
        val array = requestArray("GET", "/api/v1/accounts")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Account(o.getString("id"), o.getString("name"), o.getLong("balanceMinor"), o.optBoolean("isPrimary", false), o.optString("type", "CHECKING"), o.optString("currency", "BRL"))
        }
    }

    fun createAccount(name: String, type: String = "CHECKING"): Account {
        val o = request("POST", "/api/v1/accounts", JSONObject().apply {
            put("name", name)
            put("type", type)
            put("openingBalanceMinor", 0)
            put("currency", "BRL")
        })
        return Account(o.getString("id"), o.getString("name"), o.getLong("balanceMinor"), o.optBoolean("isPrimary", false), o.optString("type", type), o.optString("currency", "BRL"))
    }

    fun setPrimaryAccount(accountId: String): Account {
        val o = request("POST", "/api/v1/accounts/$accountId/primary")
        return Account(o.getString("id"), o.getString("name"), o.getLong("balanceMinor"), o.optBoolean("isPrimary", false), o.optString("type", "CHECKING"), o.optString("currency", "BRL"))
    }

    fun createTransfer(sourceAccountId: String, destinationAccountId: String, amountMinor: Long) {
        request("POST", "/api/v1/transfers", JSONObject().apply {
            put("sourceAccountId", sourceAccountId)
            put("destinationAccountId", destinationAccountId)
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("occurredOn", java.time.LocalDate.now().toString())
            put("source", "MANUAL")
        })
    }

    fun categories(type: String? = null): List<Category> {
        val path = "/api/v1/categories" + (type?.let { "?type=$it" } ?: "")
        val array = requestArray("GET", path)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Category(o.getString("id"), o.getString("name"), o.getString("type"), o.optString("iconKey").ifBlank { null })
        }
    }

    fun cards(): List<Card> {
        val array = requestArray("GET", "/api/v1/cards")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Card(
                o.getString("id"),
                o.getString("liabilityAccountId"),
                o.getString("name"),
                o.getString("brand"),
                o.getString("lastFourDigits"),
                o.getLong("creditLimitMinor"),
                o.getLong("availableLimitMinor"),
                o.getString("currency"),
                o.getInt("closingDay"),
                o.getInt("dueDay"),
                o.getString("status"),
            )
        }
    }

    fun createCard(
        liabilityAccountId: String,
        name: String,
        brand: String,
        lastFourDigits: String,
        creditLimitMinor: Long,
        closingDay: Int,
        dueDay: Int,
    ): Card {
        val o = request("POST", "/api/v1/cards", JSONObject().apply {
            put("liabilityAccountId", liabilityAccountId)
            put("name", name)
            put("brand", brand)
            put("lastFourDigits", lastFourDigits)
            put("creditLimitMinor", creditLimitMinor)
            put("currency", "BRL")
            put("closingDay", closingDay)
            put("dueDay", dueDay)
        })
        return Card(
            o.getString("id"), o.getString("liabilityAccountId"), o.getString("name"),
            o.getString("brand"), o.getString("lastFourDigits"), o.getLong("creditLimitMinor"),
            o.getLong("availableLimitMinor"), o.getString("currency"), o.getInt("closingDay"),
            o.getInt("dueDay"), o.getString("status")
        )
    }

    fun invoices(): List<Invoice> {
        val array = requestArray("GET", "/api/v1/invoices")
        return (0 until array.length()).map { i -> invoiceFromJson(array.getJSONObject(i)) }
    }

    fun closeInvoice(invoiceId: String): Invoice =
        invoiceFromJson(request("POST", "/api/v1/invoices/$invoiceId/close"))

    fun payInvoice(invoiceId: String, sourceAccountId: String, amountMinor: Long): Invoice {
        return invoiceFromJson(request("POST", "/api/v1/invoices/$invoiceId/payments", JSONObject().apply {
            put("sourceAccountId", sourceAccountId)
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("occurredOn", java.time.LocalDate.now().toString())
            put("source", "MANUAL")
        }))
    }

    fun createInvoice(cardId: String, periodStart: String, periodEnd: String, dueOn: String): Invoice =
        invoiceFromJson(request("POST", "/api/v1/invoices", JSONObject().apply {
            put("cardId", cardId)
            put("periodStart", periodStart)
            put("periodEnd", periodEnd)
            put("dueOn", dueOn)
        }))

    private fun invoiceFromJson(o: JSONObject): Invoice = Invoice(
        o.getString("id"), o.getString("cardId"), o.getString("periodStart"), o.getString("periodEnd"),
        o.getString("dueOn"), o.getLong("totalAmountMinor"), o.getLong("paidAmountMinor"),
        o.getLong("remainingAmountMinor"), o.getString("currency"), o.getString("status"),
        o.optJSONArray("transactionIds")?.let { a -> (0 until a.length()).map(a::getString) } ?: emptyList(),
        o.optJSONArray("paymentTransactionIds")?.let { a -> (0 until a.length()).map(a::getString) } ?: emptyList()
    )

    fun transactions(): List<Tx> {
        val array = requestArray("GET", "/api/v1/transactions")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Tx(
                o.getString("id"),
                o.getString("accountId"),
                o.optString("description").ifBlank { o.getString("type") },
                o.getLong("amountMinor"),
                o.getString("direction"),
                o.getString("occurredOn"),
                o.optString("categoryId").ifBlank { null },
                o.optString("cardId").ifBlank { null },
                o.optString("invoiceId").ifBlank { null },
            )
        }
    }

    fun budgets(): List<Budget> {
        val array = requestArray("GET", "/api/v1/budgets")
        return (0 until array.length()).map { i -> budgetFromJson(array.getJSONObject(i)) }
    }

    fun createBudget(
        amountMinor: Long,
        startOn: String,
        endOn: String,
        categoryId: String? = null,
        description: String? = null,
    ): Budget {
        return budgetFromJson(request("POST", "/api/v1/budgets", JSONObject().apply {
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("startOn", startOn)
            put("endOn", endOn)
            put("categoryId", categoryId ?: JSONObject.NULL)
            put("description", description ?: JSONObject.NULL)
        }))
    }

    fun pauseBudget(id: String): Budget =
        budgetFromJson(request("POST", "/api/v1/budgets/$id/pause"))

    fun resumeBudget(id: String): Budget =
        budgetFromJson(request("POST", "/api/v1/budgets/$id/resume"))

    fun cancelBudget(id: String): Budget =
        budgetFromJson(request("DELETE", "/api/v1/budgets/$id"))

    private fun budgetFromJson(o: JSONObject): Budget = Budget(
        id = o.getString("id"),
        amountMinor = o.getLong("amountMinor"),
        consumedMinor = o.getLong("consumedMinor"),
        remainingMinor = o.getLong("remainingMinor"),
        progressPercent = o.getInt("progressPercent"),
        alertLevel = o.getString("alertLevel"),
        currency = o.getString("currency"),
        startOn = o.getString("startOn"),
        endOn = o.getString("endOn"),
        categoryId = o.optString("categoryId").ifBlank { null },
        description = o.optString("description").ifBlank { null },
        status = o.getString("status"),
        projectedDate = o.optString("projectedDate").ifBlank { null },
        averageMonthlyContributionMinor = o.optLong("averageMonthlyContributionMinor", 0L).takeIf { o.has("averageMonthlyContributionMinor") && !o.isNull("averageMonthlyContributionMinor") },
        projectionStatus = o.optString("projectionStatus").ifBlank { null },
    )

    fun goals(): List<Goal> {
        val array = requestArray("GET", "/api/v1/goals")
        return (0 until array.length()).map { i -> goalFromJson(array.getJSONObject(i)) }
    }

    fun createGoal(
        name: String,
        targetAmountMinor: Long,
        targetDate: String?,
        description: String?,
    ): Goal {
        return goalFromJson(request("POST", "/api/v1/goals", JSONObject().apply {
            put("name", name)
            put("targetAmountMinor", targetAmountMinor)
            put("currency", "BRL")
            put("targetDate", targetDate ?: JSONObject.NULL)
            put("description", description ?: JSONObject.NULL)
        }))
    }

    fun contributeGoal(id: String, amountMinor: Long, occurredOn: String): Goal {
        val detail = request("POST", "/api/v1/goals/$id/contributions", JSONObject().apply {
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("occurredOn", occurredOn)
        })
        return goalFromJson(detail.getJSONObject("goal"))
    }

    fun pauseGoal(id: String): Goal =
        goalFromJson(request("POST", "/api/v1/goals/$id/pause"))

    fun resumeGoal(id: String): Goal =
        goalFromJson(request("POST", "/api/v1/goals/$id/resume"))

    fun cancelGoal(id: String): Goal =
        goalFromJson(request("DELETE", "/api/v1/goals/$id"))

    private fun goalFromJson(o: JSONObject): Goal = Goal(
        id = o.getString("id"),
        name = o.getString("name"),
        targetAmountMinor = o.getLong("targetAmountMinor"),
        currentAmountMinor = o.getLong("currentAmountMinor"),
        remainingMinor = o.getLong("remainingMinor"),
        progressPercent = o.getInt("progressPercent"),
        currency = o.getString("currency"),
        targetDate = o.optString("targetDate").ifBlank { null },
        description = o.optString("description").ifBlank { null },
        status = o.getString("status"),
        projectedDate = o.optString("projectedDate").ifBlank { null },
        averageMonthlyContributionMinor = if (o.isNull("averageMonthlyContributionMinor")) null else o.getLong("averageMonthlyContributionMinor"),
        projectionStatus = o.optString("projectionStatus", "NO_CONTRIBUTION"),
    )

    fun notifications(unreadOnly: Boolean = false): List<NexaNotification> {
        val path = "/api/v1/notifications?unreadOnly=$unreadOnly"
        val array = requestArray("GET", path)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            NexaNotification(
                id = o.getString("id"),
                type = o.getString("type"),
                severity = o.getString("severity"),
                title = o.getString("title"),
                message = o.getString("message"),
                readAt = o.optString("readAt").ifBlank { null },
                createdAt = o.getString("createdAt"),
            )
        }
    }

    fun markNotificationRead(id: String) {
        raw("POST", "/api/v1/notifications/$id/read", null)
    }

    fun aiAnalysis(month: String = java.time.YearMonth.now().toString()): AiAnalysis {
        val o = request("GET", "/api/v1/ai/analysis?month=$month")
        val array = o.optJSONArray("insights")
        val insights = if (array == null) emptyList() else {
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                val sources = item.optJSONArray("sourceFactIds")
                AiInsight(
                    id = item.getString("id"),
                    level = item.getString("level"),
                    title = item.getString("title"),
                    message = item.getString("message"),
                    sourceFactIds = if (sources == null) emptyList() else (0 until sources.length()).map { index -> sources.getString(index) },
                )
            }
        }
        return AiAnalysis(
            month = o.getString("month"),
            provider = o.getString("provider"),
            monthlySummary = o.getString("monthlySummary"),
            contextFactCount = o.getInt("contextFactCount"),
            insights = insights,
        )
    }

    fun askAi(question: String, month: String = java.time.YearMonth.now().toString()): AiAnswer {
        val o = request("POST", "/api/v1/ai/questions", JSONObject().apply {
            put("question", question)
            put("month", month)
        })
        val sources = o.optJSONArray("sourceFactIds")
        return AiAnswer(
            answer = o.getString("answer"),
            sourceFactIds = if (sources == null) emptyList() else (0 until sources.length()).map { i -> sources.getString(i) },
        )
    }

    fun recurrences(): List<Recurrence> {
        val array = requestArray("GET", "/api/v1/recurrences")
        return (0 until array.length()).map { i -> recurrenceFromJson(array.getJSONObject(i)) }
    }

    fun createRecurrence(
        accountId: String,
        amountMinor: Long,
        transactionType: String,
        frequency: String,
        interval: Int,
        startOn: String,
        endOn: String? = null,
        maxOccurrences: Int? = null,
        description: String? = null,
        categoryId: String? = null,
    ): Recurrence = recurrenceFromJson(request("POST", "/api/v1/recurrences", JSONObject().apply {
        put("accountId", accountId)
        put("amountMinor", amountMinor)
        put("currency", "BRL")
        put("transactionType", transactionType)
        put("frequency", frequency)
        put("interval", interval)
        put("startOn", startOn)
        put("endOn", endOn ?: JSONObject.NULL)
        put("maxOccurrences", maxOccurrences ?: JSONObject.NULL)
        put("description", description ?: JSONObject.NULL)
        put("categoryId", categoryId ?: JSONObject.NULL)
    }))

    fun updateRecurrence(
        id: String,
        accountId: String,
        amountMinor: Long,
        transactionType: String,
        frequency: String,
        interval: Int,
        startOn: String,
        endOn: String? = null,
        maxOccurrences: Int? = null,
        description: String? = null,
        categoryId: String? = null,
    ): Recurrence = recurrenceFromJson(request("PUT", "/api/v1/recurrences/$id", JSONObject().apply {
        put("accountId", accountId)
        put("amountMinor", amountMinor)
        put("currency", "BRL")
        put("transactionType", transactionType)
        put("frequency", frequency)
        put("interval", interval)
        put("startOn", startOn)
        put("endOn", endOn ?: JSONObject.NULL)
        put("maxOccurrences", maxOccurrences ?: JSONObject.NULL)
        put("description", description ?: JSONObject.NULL)
        put("categoryId", categoryId ?: JSONObject.NULL)
    }))

    fun pauseRecurrence(id: String): Recurrence =
        recurrenceFromJson(request("POST", "/api/v1/recurrences/$id/pause"))

    fun resumeRecurrence(id: String): Recurrence =
        recurrenceFromJson(request("POST", "/api/v1/recurrences/$id/resume"))

    fun cancelRecurrence(id: String): Recurrence =
        recurrenceFromJson(request("DELETE", "/api/v1/recurrences/$id"))

    fun recurrenceCandidates(): List<RecurrenceCandidate> {
        val array = requestArray("GET", "/api/v1/recurrences/candidates")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            RecurrenceCandidate(
                accountId = o.getString("accountId"),
                amountMinor = o.getLong("amountMinor"),
                currency = o.getString("currency"),
                transactionType = o.getString("transactionType"),
                frequency = o.getString("frequency"),
                description = o.getString("description"),
                categoryId = o.optString("categoryId").ifBlank { null },
                occurrenceCount = o.getInt("occurrenceCount"),
                confidencePercent = o.getInt("confidencePercent"),
                nextOccurrenceOn = o.getString("nextOccurrenceOn"),
            )
        }
    }

    private fun recurrenceFromJson(o: JSONObject): Recurrence {
        val occurrences = o.optJSONArray("nextOccurrences")
        val nextOccurrences = if (occurrences == null) emptyList() else {
            (0 until occurrences.length()).map { i ->
                val item = occurrences.getJSONObject(i)
                item.getInt("number").toString() + "|" + item.getString("scheduledOn")
            }
        }
        return Recurrence(
            id = o.getString("id"),
            accountId = o.getString("accountId"),
            amountMinor = o.getLong("amountMinor"),
            transactionType = o.getString("transactionType"),
            frequency = o.getString("frequency"),
            interval = o.getInt("interval"),
            startOn = o.getString("startOn"),
            endOn = o.optString("endOn").ifBlank { null },
            maxOccurrences = o.optInt("maxOccurrences", 0).takeIf { o.has("maxOccurrences") && !o.isNull("maxOccurrences") && it > 0 },
            description = o.optString("description").ifBlank { "Recorrência" },
            categoryId = o.optString("categoryId").ifBlank { null },
            status = o.getString("status"),
            nextOccurrenceNumber = o.optInt("nextOccurrenceNumber", 0).takeIf { it > 0 },
            nextOccurrenceOn = o.optString("nextOccurrenceOn").ifBlank { null },
            nextOccurrences = nextOccurrences,
            alertLevel = o.optString("alertLevel", "NONE"),
        )
    }

    fun createTransaction(
        accountId: String,
        amountMinor: Long,
        type: String,
        direction: String,
        description: String,
        categoryId: String? = null,
        id: String? = null,
        occurredOn: String = java.time.LocalDate.now().toString(),
    ): Tx {
        val o = request("POST", "/api/v1/transactions", JSONObject().apply {
            put("accountId", accountId)
            put("id", id ?: JSONObject.NULL)
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("type", type)
            put("direction", direction)
            put("occurredOn", occurredOn)
            put("description", description)
            put("categoryId", categoryId ?: JSONObject.NULL)
            put("source", "MANUAL")
        })
        return Tx(
            o.getString("id"),
            o.getString("accountId"),
            o.optString("description").ifBlank { type },
            o.getLong("amountMinor"),
            o.getString("direction"),
            o.getString("occurredOn"),
            o.optString("categoryId").ifBlank { null }
        )
    }

    fun createCardPurchase(
        cardId: String,
        amountMinor: Long,
        description: String,
        installmentCount: Int = 1,
    ): Tx {
        val o = request("POST", "/api/v1/cards/$cardId/purchases", JSONObject().apply {
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("occurredOn", java.time.LocalDate.now().toString())
            put("description", description)
            put("source", "MANUAL")
            put("post", true)
            put("installmentCount", installmentCount)
        })
        return Tx(
            o.getString("id"),
            o.getString("accountId"),
            o.optString("description").ifBlank { "Compra no cartão" },
            o.getLong("amountMinor"),
            o.getString("direction"),
            o.getString("occurredOn"),
            o.optString("categoryId").ifBlank { null },
            o.optString("cardId").ifBlank { null },
            o.optString("invoiceId").ifBlank { null },
            o.optString("installmentId").ifBlank { null },
            o.optInt("installmentNumber", 0).takeIf { it > 0 },
        )
    }

    fun pushSync(operations: List<com.nexa.finance.sync.SyncOperationEntity>): com.nexa.finance.sync.SyncPushResult {
        val body = JSONObject().put("operations", org.json.JSONArray().apply {
            operations.forEach { operation ->
                put(JSONObject().apply {
                    put("operationId", operation.id)
                    put("entityType", operation.entityType)
                    put("entityId", operation.entityId)
                    put("operation", "UPSERT")
                    put("payload", JSONObject(operation.payloadJson).apply { put("id", operation.entityId) })
                })
            }
        })
        val response = request("POST", "/api/v1/sync/push", body)
        val acks = response.getJSONArray("acknowledgements")
        return com.nexa.finance.sync.SyncPushResult(
            (0 until acks.length()).map { i ->
                val item = acks.getJSONObject(i)
                com.nexa.finance.sync.SyncPushAck(item.getString("operationId"), item.getString("entityId"), item.getString("status"))
            },
            response.getLong("cursor"),
        )
    }

    fun pullSync(cursor: Long, limit: Int = 50): com.nexa.finance.sync.SyncPullResult {
        val response = request("GET", "/api/v1/sync/pull?cursor=$cursor&limit=$limit")
        val changes = response.getJSONArray("changes")
        return com.nexa.finance.sync.SyncPullResult(
            response.getLong("cursor"),
            (0 until changes.length()).map { i ->
                val item = changes.getJSONObject(i)
                val txJson = item.optJSONObject("transaction")
                val tx = txJson?.let { o ->
                    com.nexa.finance.sync.SyncTransaction(
                        id = o.getString("id"),
                        accountId = o.getString("accountId"),
                        amountMinor = o.getLong("amountMinor"),
                        currency = o.getString("currency"),
                        type = o.getString("type"),
                        direction = o.getString("direction"),
                        status = o.getString("status"),
                        occurredOn = o.getString("occurredOn"),
                        description = o.optString("description").ifBlank { null },
                        categoryId = o.optString("categoryId").ifBlank { null },
                        source = o.getString("source"),
                        installmentId = o.optString("installmentId").ifBlank { null },
                        installmentNumber = o.optInt("installmentNumber", 0).takeIf { it > 0 },
                    )
                }
                com.nexa.finance.sync.SyncPullChange(item.getLong("cursor"), item.getString("entityId"), item.getString("operation"), tx)
            },
        )
    }

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        JSONObject(raw(method, path, body))

    private fun requestArray(method: String, path: String): JSONArray =
        JSONArray(raw(method, path, null))

    private fun raw(method: String, path: String, body: JSONObject?): String {
        val c = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            body?.toString()?.toByteArray(Charsets.UTF_8)?.let { bytes ->
                c.outputStream.use { stream -> stream.write(bytes) }
            }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = if (text.isBlank()) {
                    "HTTP $status"
                } else {
                    runCatching { JSONObject(text).optString("message").ifBlank { "HTTP $status" } }
                        .getOrDefault("HTTP $status")
                }
                throw IllegalStateException(message)
            }
            if (text.isBlank()) {
                throw IllegalStateException("HTTP $status returned an empty body.")
            }
            return text
        } finally {
            c.disconnect()
        }
    }
}

data class Account(val id: String, val name: String, val balanceMinor: Long, val isPrimary: Boolean = false, val type: String = "CHECKING", val currency: String = "BRL")
data class Card(
    val id: String,
    val liabilityAccountId: String,
    val name: String,
    val brand: String,
    val lastFourDigits: String,
    val creditLimitMinor: Long,
    val availableLimitMinor: Long,
    val currency: String,
    val closingDay: Int,
    val dueDay: Int,
    val status: String,
)

data class Invoice(
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
data class Category(val id: String, val name: String, val type: String, val iconKey: String?)
data class Tx(
    val id: String,
    val accountId: String,
    val description: String,
    val amountMinor: Long,
    val direction: String,
    val date: String,
    val categoryId: String?,
    val cardId: String? = null,
    val invoiceId: String? = null,
    val installmentId: String? = null,
    val installmentNumber: Int? = null,
)

data class Budget(
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
    val projectedDate: String? = null,
    val averageMonthlyContributionMinor: Long? = null,
    val projectionStatus: String? = null,
)

data class Goal(
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

data class Recurrence(
    val id: String,
    val accountId: String,
    val amountMinor: Long,
    val transactionType: String,
    val frequency: String,
    val interval: Int,
    val startOn: String,
    val endOn: String?,
    val maxOccurrences: Int?,
    val description: String,
    val categoryId: String?,
    val status: String,
    val nextOccurrenceNumber: Int?,
    val nextOccurrenceOn: String?,
    val nextOccurrences: List<String>,
    val alertLevel: String,
)

data class RecurrenceCandidate(
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val transactionType: String,
    val frequency: String,
    val description: String,
    val categoryId: String?,
    val occurrenceCount: Int,
    val confidencePercent: Int,
    val nextOccurrenceOn: String,
)

data class AiInsight(
    val id: String,
    val level: String,
    val title: String,
    val message: String,
    val sourceFactIds: List<String>,
)

data class AiAnalysis(
    val month: String,
    val provider: String,
    val monthlySummary: String,
    val contextFactCount: Int,
    val insights: List<AiInsight>,
)

data class AiAnswer(
    val answer: String,
    val sourceFactIds: List<String>,
)
data class NexaNotification(
    val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val message: String,
    val readAt: String?,
    val createdAt: String,
)