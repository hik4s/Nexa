package com.nexa.finance.infrastructure.card

import com.nexa.finance.domain.card.Card
import com.nexa.finance.domain.card.CardBrand
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.card.CardStatus
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.util.UUID

class JdbcCardRepository(private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider()) {
    fun create(card: Card) {
        connections.open().use { c ->
            c.prepareStatement("INSERT INTO cards (id, owner_id, liability_account_id, name, brand, last_four_digits, credit_limit_minor, currency, closing_day, due_day, status, created_at, updated_at) SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? WHERE EXISTS (SELECT 1 FROM accounts WHERE id = ? AND owner_id = ? AND type = 'CREDIT_CARD' AND status = 'ACTIVE')").use { s ->
                s.setObject(1, card.id.value); s.setObject(2, card.ownerId.value); s.setObject(3, card.liabilityAccountId.value); s.setString(4, card.name); s.setString(5, card.brand.name); s.setString(6, card.lastFourDigits); s.setLong(7, card.creditLimit.amountInMinor); s.setString(8, card.creditLimit.currency.name); s.setInt(9, card.closingDay); s.setInt(10, card.dueDay); s.setString(11, card.status.name); s.setTimestamp(12, Timestamp.from(card.createdAt)); s.setTimestamp(13, Timestamp.from(card.updatedAt)); s.setObject(14, card.liabilityAccountId.value); s.setObject(15, card.ownerId.value)
                if (s.executeUpdate() != 1) throw DomainValidationException("Liability account was not found for this user.")
            }
        }
    }

    fun findByOwner(ownerId: UserId): List<Card> = connections.open().use { c -> c.prepareStatement("SELECT * FROM cards WHERE owner_id = ? ORDER BY created_at, id").use { s -> s.setObject(1, ownerId.value); s.executeQuery().use { r -> buildList { while (r.next()) add(r.toCard()) } } } }
    fun findByOwnerAndId(ownerId: UserId, id: CardId): Card? = connections.open().use { c -> c.prepareStatement("SELECT * FROM cards WHERE owner_id = ? AND id = ?").use { s -> s.setObject(1, ownerId.value); s.setObject(2, id.value); s.executeQuery().use { r -> if (r.next()) r.toCard() else null } } }
    fun updateStatus(ownerId: UserId, id: CardId, status: CardStatus): Card? = connections.open().use { c -> c.prepareStatement("UPDATE cards SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE owner_id = ? AND id = ? RETURNING *").use { s -> s.setString(1, status.name); s.setObject(2, ownerId.value); s.setObject(3, id.value); s.executeQuery().use { r -> if (r.next()) r.toCard() else null } } }

    private fun java.sql.ResultSet.toCard(): Card = Card.restore(
        id = CardId(getObject("id", UUID::class.java)), ownerId = UserId(getObject("owner_id", UUID::class.java)), liabilityAccountId = AccountId(getObject("liability_account_id", UUID::class.java)), name = getString("name"), brand = CardBrand.valueOf(getString("brand")), lastFourDigits = getString("last_four_digits"), creditLimit = Money(getLong("credit_limit_minor"), CurrencyCode.valueOf(getString("currency"))), closingDay = getInt("closing_day"), dueDay = getInt("due_day"), status = CardStatus.valueOf(getString("status")), createdAt = getTimestamp("created_at").toInstant(), updatedAt = getTimestamp("updated_at").toInstant()
    )
}