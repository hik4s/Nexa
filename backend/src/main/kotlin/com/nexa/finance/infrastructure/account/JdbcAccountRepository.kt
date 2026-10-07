package com.nexa.finance.infrastructure.account

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountStatus
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.util.UUID

class JdbcAccountRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun create(account: Account) {
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO accounts
                    (id, owner_id, name, type, currency, opening_balance_minor, status, is_primary, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, account.id.value)
                s.setObject(2, account.ownerId.value)
                s.setString(3, account.name)
                s.setString(4, account.type.name)
                s.setString(5, account.currency.name)
                s.setLong(6, account.openingBalance.amountInMinor)
                s.setString(7, account.status.name)
                s.setBoolean(8, account.isPrimary)
                s.setTimestamp(9, Timestamp.from(account.createdAt))
                s.setTimestamp(10, Timestamp.from(account.updatedAt))
                s.executeUpdate()
            }
        }
    }

    fun findByOwner(ownerId: UserId): List<Account> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, owner_id, name, type, currency, opening_balance_minor, status, is_primary, created_at, updated_at
                FROM accounts
                WHERE owner_id = ?
                ORDER BY created_at, id
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toAccount())
                    }
                }
            }
        }

    fun findByOwnerAndId(ownerId: UserId, accountId: AccountId): Account? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, owner_id, name, type, currency, opening_balance_minor, status, is_primary, created_at, updated_at
                FROM accounts
                WHERE owner_id = ? AND id = ?
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, accountId.value)
                s.executeQuery().use { r ->
                    if (r.next()) r.toAccount() else null
                }
            }
        }

    fun setPrimary(ownerId: UserId, accountId: AccountId): Account? =
        connections.open().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("UPDATE accounts SET is_primary = FALSE, updated_at = CURRENT_TIMESTAMP WHERE owner_id = ? AND is_primary = TRUE").use { s ->
                    s.setObject(1, ownerId.value)
                    s.executeUpdate()
                }
                c.prepareStatement("UPDATE accounts SET is_primary = TRUE, updated_at = CURRENT_TIMESTAMP WHERE owner_id = ? AND id = ? AND status = 'ACTIVE'").use { s ->
                    s.setObject(1, ownerId.value)
                    s.setObject(2, accountId.value)
                    if (s.executeUpdate() == 0) {
                        c.rollback()
                        return@use null
                    }
                }
                c.commit()
                findByOwnerAndId(ownerId, accountId)
            } catch (e: Exception) {
                c.rollback()
                throw e
            } finally {
                c.autoCommit = true
            }
        }

    fun balance(account: Account): Money =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN direction = 'CREDIT' THEN amount_minor
                        WHEN direction = 'DEBIT' THEN -amount_minor
                        ELSE 0
                    END
                ), 0)
                FROM transactions
                WHERE account_id = ? AND status = 'POSTED'
                """.trimIndent()
            ).use { s ->
                s.setObject(1, account.id.value)
                s.executeQuery().use { r ->
                    r.next()
                    account.openingBalance + Money(r.getLong(1), account.currency)
                }
            }
        }

    private fun java.sql.ResultSet.toAccount(): Account =
        Account.restore(
            id = AccountId(getObject("id", UUID::class.java)),
            ownerId = UserId(getObject("owner_id", UUID::class.java)),
            name = getString("name"),
            type = AccountType.valueOf(getString("type")),
            openingBalance = Money(
                getLong("opening_balance_minor"),
                CurrencyCode.valueOf(getString("currency")),
            ),
            createdAt = getTimestamp("created_at").toInstant(),
            updatedAt = getTimestamp("updated_at").toInstant(),
            status = AccountStatus.valueOf(getString("status")),
            isPrimary = getBoolean("is_primary"),
        )
}
