package com.nexa.finance

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private val baseUrl: String = BuildConfig.NEXA_API_URL) {
    var token: String? = null

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

    fun recurrences(): List<Recurrence> {
        val array = requestArray("GET", "/api/v1/recurrences")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Recurrence(
                o.getString("id"),
                o.getLong("amountMinor"),
                o.getString("transactionType"),
                o.getString("frequency"),
                o.getInt("interval"),
                o.getString("startOn"),
                o.optString("description").ifBlank { "Recorrência" },
                o.getString("status"),
            )
        }
    }

    fun createTransaction(
        accountId: String,
        amountMinor: Long,
        type: String,
        direction: String,
        description: String,
        categoryId: String? = null,
    ): Tx {
        val o = request("POST", "/api/v1/transactions", JSONObject().apply {
            put("accountId", accountId)
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("type", type)
            put("direction", direction)
            put("occurredOn", java.time.LocalDate.now().toString())
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
    val amountMinor: Long,
    val transactionType: String,
    val frequency: String,
    val interval: Int,
    val startOn: String,
    val description: String,
    val status: String,
)
