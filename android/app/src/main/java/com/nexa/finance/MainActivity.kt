package com.nexa.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexa.finance.ui.theme.NexaTheme
import com.nexa.finance.sync.LocalDatabaseProvider
import com.nexa.finance.sync.SessionStore
import com.nexa.finance.sync.SyncQueue
import com.nexa.finance.sync.SyncScheduler
import com.nexa.finance.sync.SyncWorker
import com.nexa.finance.ui.QuickEntryCard
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val api = ApiClient()
    private lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionStore = SessionStore(this)
        sessionStore.token()?.let(api::withToken)
        setContent { NexaTheme { NexaApp(api, sessionStore) } }
    }
}

@Composable
private fun NexaApp(api: ApiClient, sessionStore: SessionStore) {
    var loggedIn by remember { mutableStateOf(sessionStore.token() != null && sessionStore.userId() != null) }
    if (loggedIn) DashboardScreen(api) { sessionStore.clear(); api.token = null; loggedIn = false } else AuthScreen(api, sessionStore) { loggedIn = true }
}

@Composable
private fun AuthScreen(api: ApiClient, sessionStore: SessionStore, onLogin: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run(action: () -> String?) {
        busy = true
        scope.launch {
            try {
                val userId = withContext(Dispatchers.IO) { action() }
                if (!register && userId != null) {
                    withContext(Dispatchers.IO) {
                        sessionStore.save(userId, api.token ?: error("Sessão inválida."))
                    }
                }
                message = if (register) "Conta criada. Faça login." else "Login realizado."
                busy = false
                if (!register) onLogin()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "Falha na operação"
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("NEXA", style = MaterialTheme.typography.displaySmall)
        Text("Finance", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        if (register) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().testTag("auth_name"), label = { Text("Nome") })
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().testTag("auth_email"), label = { Text("E-mail") })
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().testTag("auth_password"), label = { Text("Senha") })
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && email.isNotBlank() && password.isNotBlank() && (!register || name.isNotBlank()),
            onClick = { run { if (register) { api.register(email, name, password); null } else api.login(email, password) } },
            modifier = Modifier.fillMaxWidth().testTag("auth_submit")
        ) { Text(if (register) "Criar conta" else "Entrar") }
        TextButton(onClick = { register = !register; message = "" }) {
            Text(if (register) "Já tenho uma conta" else "Criar uma conta")
        }
        if (message.isNotBlank()) Text(message, modifier = Modifier.padding(top = 8.dp).testTag("auth_message"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dashboard(api: ApiClient, sessionStore: SessionStore, logout: () -> Unit) {
    var accounts by remember { mutableStateOf(emptyList<Account>()) }
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var categories by remember { mutableStateOf(emptyList<Category>()) }
    var transactions by remember { mutableStateOf(emptyList<Tx>()) }
    var message by remember { mutableStateOf("Carregando...") }
    var accountName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expense by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun <T> loadSafely(loader: () -> T): Result<T> =
        try {
            Result.success(withContext(Dispatchers.IO) { loader() })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }

    suspend fun refresh() = coroutineScope {
        val accountsDeferred = async { loadSafely { api.accounts() } }
        val categoriesDeferred = async { loadSafely { api.categories() } }
        val cardsDeferred = async { loadSafely { api.cards() } }
        val transactionsDeferred = async { loadSafely { api.transactions() } }

        val accountsResult = accountsDeferred.await()
        val categoriesResult = categoriesDeferred.await()
        val cardsResult = cardsDeferred.await()
        val transactionsResult = transactionsDeferred.await()

        accountsResult.getOrNull()?.let { accounts = it }
        categoriesResult.getOrNull()?.let { categories = it }
        cardsResult.getOrNull()?.let { cards = it }
        transactionsResult.getOrNull()?.let { transactions = it }

        val failures = listOf(accountsResult, categoriesResult, cardsResult, transactionsResult).count { it.isFailure }
        message = if (failures == 0) "" else "Alguns dados não puderam ser carregados."
    }

    LaunchedEffect(Unit) { refresh() }

    val balance = accounts.sumOf { it.balanceMinor }
    Scaffold(topBar = {
        TopAppBar(title = { Text("NEXA Finance") }, actions = { TextButton(onClick = logout) { Text("Sair") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().testTag("dashboard_list").padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Saldo total", style = MaterialTheme.typography.labelLarge)
                Text(formatMoney(balance), style = MaterialTheme.typography.headlineMedium)
            }
            item {
                QuickEntryCard(
                    api = api,
                    accounts = accounts,
                    cards = cards,
                    categories = categories,
                    onTransactionAdded = { transactions = listOf(it) + transactions },
                    onMessage = { message = it },
                )
            }
            item { Text("Contas", style = MaterialTheme.typography.titleLarge) }
            items(accounts) { account ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        Text(formatMoney(account.balanceMinor))
                    }
                }
            }
            item {
                OutlinedTextField(accountName, { accountName = it }, Modifier.fillMaxWidth().testTag("account_name"), label = { Text("Nova conta") })
                Button(modifier = Modifier.testTag("account_submit"), enabled = accountName.isNotBlank(), onClick = {
                    val nameToCreate = accountName
                    scope.launch {
                        try {
                            val created = withContext(Dispatchers.IO) { api.createAccount(nameToCreate) }
                            accounts = accounts + created
                            accountName = ""
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            message = error.message ?: "Erro"
                        }
                    }
                }) { Text("Adicionar conta") }
            }
            item { HorizontalDivider(); Text("Lançamento manual", style = MaterialTheme.typography.titleLarge) }
            item {
                Column(Modifier.testTag("transaction_form")) {
                    if (accounts.isEmpty()) Text("Crie uma conta antes de lançar receitas ou despesas.")
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth().testTag("tx_amount"), label = { Text("Valor (R$)") })
                    OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth().testTag("tx_description"), label = { Text("Descrição") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Despesa")
                        Switch(expense, { expense = it })
                        Text(if (expense) "Despesa" else "Receita")
                    }
                    Button(modifier = Modifier.testTag("tx_submit"), enabled = accounts.isNotEmpty() && amount.replace(",", ".").toDoubleOrNull()?.let { it > 0 } == true, onClick = {
                        val cents = ((amount.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val account = accounts.first()
                        val descriptionToCreate = description.ifBlank { if (expense) "Despesa" else "Receita" }
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    api.createTransaction(
                                        account.id,
                                        cents,
                                        if (expense) "EXPENSE" else "INCOME",
                                        if (expense) "DEBIT" else "CREDIT",
                                        descriptionToCreate,
                                    )
                                }
                                amount = ""
                                description = ""
                                message = "Transação registrada."
                                runCatching {
                                    withContext(Dispatchers.IO) { api.transactions() }
                                }.onSuccess { latest ->
                                    transactions = latest
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                message = error.message ?: "Erro"
                            }
                        }
                    }) { Text("Registrar") }
                    if (message.isNotBlank()) Text(message, modifier = Modifier.testTag("transaction_status"))
                }
            }
            item { Text("Histórico", style = MaterialTheme.typography.titleLarge) }
            items(transactions) { tx ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.titleMedium)
                            Text(tx.date, style = MaterialTheme.typography.bodySmall)
                        }
                        Text((if (tx.direction == "DEBIT") "-" else "+") + formatMoney(tx.amountMinor), modifier = Modifier.testTag("tx_history_amount"))
                    }
                }
            }
            if (message.isNotBlank()) item { Text(message) }
        }
    }
}

private fun formatMoney(minor: Long): String = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", minor / 100.0)
