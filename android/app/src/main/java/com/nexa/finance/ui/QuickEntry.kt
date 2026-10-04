package com.nexa.finance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexa.finance.*
import java.text.Normalizer
import java.util.Locale

private data class ParsedQuickEntry(
    val amountMinor: Long?,
    val description: String,
    val isIncome: Boolean,
    val categoryHint: String,
)

private fun normalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase(Locale.ROOT)

private fun parseQuickEntry(input: String): ParsedQuickEntry {
    val raw = input.trim()
    val amountRegex = Regex("""(?i)(?:r\$\s*)?(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})|\d+(?:[.,]\d{1,2})?)""")
    val match = amountRegex.find(raw)
    val amountMinor = match?.groupValues?.getOrNull(1)?.let { value ->
        val normalized = if (value.contains(",") && value.contains(".")) {
            value.replace(".", "").replace(",", ".")
        } else {
            value.replace(",", ".")
        }
        runCatching { normalized.toBigDecimal().movePointRight(2).longValueExact() }.getOrNull()
    }
    val description = raw.replace(match?.value.orEmpty(), "").trim().trim('-', '–', ':')
    val normalized = normalize(description)
    val income = listOf(
        "salario", "recebi", "recebimento", "freela", "venda",
        "pagamento recebido", "renda", "pix recebido"
    ).any { normalized.contains(normalize(it)) }

    val category = when {
        listOf("almoço", "almoco", "comida", "restaurante", "lanche", "mercado", "supermercado", "ifood")
            .any { normalized.contains(normalize(it)) } -> "Alimentação"
        listOf("uber", "gasolina", "combustivel", "onibus", "transporte", "estacionamento")
            .any { normalized.contains(normalize(it)) } -> "Transporte"
        listOf("aluguel", "condominio", "luz", "energia", "agua", "casa")
            .any { normalized.contains(normalize(it)) } -> "Moradia"
        listOf("farmacia", "medico", "consulta", "dentista", "saude")
            .any { normalized.contains(normalize(it)) } -> "Saúde"
        listOf("cinema", "jogo", "festa", "bar", "viagem", "lazer")
            .any { normalized.contains(normalize(it)) } -> "Lazer"
        listOf("roupa", "shopping", "presente", "compra", "compras")
            .any { normalized.contains(normalize(it)) } -> "Compras"
        listOf("curso", "faculdade", "escola", "livro", "educacao")
            .any { normalized.contains(normalize(it)) } -> "Educação"
        listOf("netflix", "spotify", "prime", "assinatura", "streaming")
            .any { normalized.contains(normalize(it)) } -> "Assinaturas"
        income -> "Salário"
        else -> "Outros"
    }

    return ParsedQuickEntry(
        amountMinor,
        description.ifBlank { if (income) "Receita" else "Despesa" },
        income,
        category
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryCard(
    api: ApiClient,
    accounts: List<Account>,
    cards: List<Card>,
    categories: List<Category>,
    onTransactionAdded: (Tx) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexa_preferences", 0) }
    var input by remember { mutableStateOf("") }
    var selectedAccountId by remember {
        mutableStateOf(prefs.getString("default_account_id", "").orEmpty())
    }
    var selectedCardId by remember {
        mutableStateOf(prefs.getString("default_card_id", "").orEmpty())
    }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var useCard by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    var cardMenu by remember { mutableStateOf(false) }
    var installmentMenu by remember { mutableStateOf(false) }
    var installmentCount by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val parsed = parseQuickEntry(input)

    LaunchedEffect(accounts) {
        if (accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull()?.id.orEmpty()
        }
    }
    LaunchedEffect(cards) {
        if (cards.none { it.id == selectedCardId }) {
            selectedCardId = cards.firstOrNull()?.id.orEmpty()
        }
    }
    LaunchedEffect(input, categories) {
        selectedCategory = categories.firstOrNull { normalize(it.name) == normalize(parsed.categoryHint) } ?: categories.firstOrNull()
    }

    fun submit() {
        if (busy || parsed.amountMinor == null || parsed.amountMinor <= 0) return
        if (!useCard && selectedAccountId.isBlank()) {
            onMessage("Crie uma conta antes de registrar.")
            return
        }
        if (useCard && selectedCardId.isBlank()) {
            onMessage("Cadastre um cartão antes de registrar no cartão.")
            return
        }
        busy = true
        Thread {
            try {
                val created = if (useCard) {
                    api.createCardPurchase(
                        selectedCardId,
                        parsed.amountMinor,
                        parsed.description,
                        installmentCount,
                    )
                } else {
                    api.createTransaction(
                        accountId = selectedAccountId,
                        amountMinor = parsed.amountMinor,
                        type = if (parsed.isIncome) "INCOME" else "EXPENSE",
                        direction = if (parsed.isIncome) "CREDIT" else "DEBIT",
                        description = parsed.description,
                        categoryId = selectedCategory?.id,
                    )
                }
                runOnMain {
                    onTransactionAdded(created)
                    input = ""
                    busy = false
                    status = "Lançamento registrado."
                    onMessage("Lançamento registrado.")
                }
            } catch (e: Exception) {
                runOnMain {
                    busy = false
                    status = e.message ?: "Erro ao registrar"
                    onMessage(status)
                }
            }
        }.start()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Registro rápido", style = MaterialTheme.typography.titleLarge)
            Text("Ex.: 18,90 almoço", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().testTag("quick_input"),
                label = { Text("Valor + descrição") },
                singleLine = true,
            )

            if (input.isNotBlank()) {
                Text(
                    (if (parsed.isIncome) "Receita" else "Despesa") + " · " + parsed.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    parsed.amountMinor?.let(::formatMoney) ?: "Informe um valor válido",
                    style = MaterialTheme.typography.titleMedium
                )

                Box {
                    OutlinedButton(
                        onClick = { categoryMenu = true },
                        modifier = Modifier.testTag("quick_category")
                    ) {
                        Text("Categoria: " + (selectedCategory?.name ?: "Outros"))
                    }
                    DropdownMenu(
                        expanded = categoryMenu,
                        onDismissRequest = { categoryMenu = false }
                    ) {
                        categories
                            .filter { it.type == "BOTH" || it.type == if (parsed.isIncome) "INCOME" else "EXPENSE" }
                            .forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryMenu = false
                                    }
                                )
                            }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useCard,
                        onClick = { useCard = false },
                        label = { Text("Conta") },
                        modifier = Modifier.testTag("quick_account_mode")
                    )
                    if (cards.isNotEmpty()) {
                        FilterChip(
                            selected = useCard,
                            onClick = { useCard = true },
                            label = { Text("Cartão") },
                            modifier = Modifier.testTag("quick_card_mode")
                        )
                    }
                }

                if (!useCard) {
                    Box {
                        OutlinedButton(
                            onClick = { accountMenu = true },
                            modifier = Modifier.testTag("quick_account")
                        ) {
                            Text(
                                "Conta: " +
                                    (accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "Selecionar")
                            )
                        }
                        DropdownMenu(
                            expanded = accountMenu,
                            onDismissRequest = { accountMenu = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        selectedAccountId = account.id
                                        prefs.edit().putString("default_account_id", account.id).apply()
                                        accountMenu = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Box {
                        OutlinedButton(
                            onClick = { cardMenu = true },
                            modifier = Modifier.testTag("quick_card")
                        ) {
                            Text(
                                "Cartão: " +
                                    (cards.firstOrNull { it.id == selectedCardId }?.name ?: "Selecionar")
                            )
                        }
                        DropdownMenu(
                            expanded = cardMenu,
                            onDismissRequest = { cardMenu = false }
                        ) {
                            cards.filter { it.status == "ACTIVE" }.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text(card.name + " · " + formatMoney(card.availableLimitMinor)) },
                                    onClick = {
                                        selectedCardId = card.id
                                        prefs.edit().putString("default_card_id", card.id).apply()
                                        cardMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        OutlinedButton(
                            onClick = { installmentMenu = true },
                            modifier = Modifier.testTag("quick_installments")
                        ) {
                            Text("Parcelas: " + if (installmentCount == 1) "À vista" else "$installmentCount x")
                        }
                        DropdownMenu(
                            expanded = installmentMenu,
                            onDismissRequest = { installmentMenu = false }
                        ) {
                            (1..12).forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(if (count == 1) "À vista" else "$count parcelas") },
                                    onClick = {
                                        installmentCount = count
                                        installmentMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = ::submit,
                    enabled = parsed.amountMinor != null && parsed.amountMinor > 0 &&
                        !busy && ((!useCard && selectedAccountId.isNotBlank()) ||
                        (useCard && selectedCardId.isNotBlank())),
                    modifier = Modifier.fillMaxWidth().testTag("quick_submit")
                ) {
                    Text(if (busy) "Registrando..." else "Confirmar lançamento")
                }
            }
            if (status.isNotBlank()) {
                Text(status, modifier = Modifier.testTag("quick_status"))
            }
        }
    }
}

private fun formatMoney(minor: Long): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", minor / 100.0)

private fun runOnMain(action: () -> Unit) =
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
