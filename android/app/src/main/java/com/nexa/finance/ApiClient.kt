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
            Account(o.getString("id"), o.getString("name"), o.getLong("balanceMinor"))
        }
    }

    fun createAccount(name: String): Account {
        val o = request("POST", "/api/v1/accounts", JSONObject().apply {
            put("name", name)
            put("type", "CHECKING")
            put("openingBalanceMinor", 0)
            put("currency", "BRL")
        })
        return Account(o.getString("id"), o.getString("name"), o.getLong("balanceMinor"))
    }

    fun transactions(): List<Tx> {
        val array = requestArray("GET", "/api/v1/transactions")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Tx(o.getString("id"), o.optString("description").ifBlank { o.getString("type") }, o.getLong("amountMinor"), o.getString("direction"), o.getString("occurredOn"))
        }
    }

    fun createTransaction(accountId: String, amountMinor: Long, type: String, direction: String, description: String) {
        request("POST", "/api/v1/transactions", JSONObject().apply {
            put("accountId", accountId)
            put("amountMinor", amountMinor)
            put("currency", "BRL")
            put("type", type)
            put("direction", direction)
            put("occurredOn", java.time.LocalDate.now().toString())
            put("description", description)
            put("source", "MANUAL")
        })
    }

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject = JSONObject(raw(method, path, body))
    private fun requestArray(method: String, path: String): JSONArray = JSONArray(raw(method, path, null))

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
            body?.toString()?.toByteArray(Charsets.UTF_8)?.let { bytes -> c.outputStream.use { stream -> stream.write(bytes) } }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException(JSONObject(text).optString("message").ifBlank { "HTTP $status" })
            return text
        } finally { c.disconnect() }
    }
}

data class Account(val id: String, val name: String, val balanceMinor: Long)
data class Tx(val id: String, val description: String, val amountMinor: Long, val direction: String, val date: String)
