package com.nexa.finance.infrastructure.export

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class JdbcDataExportRepository(private val connections: DatabaseConnectionProvider) {
    fun export(ownerId: UserId): JsonObject? = connections.open().use { connection ->
        connection.prepareStatement(SQL).use { statement ->
            statement.setObject(1, ownerId.value)
            statement.executeQuery().use { result ->
                if (!result.next()) null
                else Json.parseToJsonElement(result.getString("export")).jsonObject
            }
        }
    }

    private companion object {
        const val SQL = """
            SELECT jsonb_build_object(
                'profile', jsonb_build_object(
                    'id', u.id,
                    'email', u.email,
                    'displayName', u.display_name,
                    'timeZone', u.time_zone,
                    'status', u.status,
                    'createdAt', u.created_at,
                    'updatedAt', u.updated_at
                ),
                'accounts', COALESCE((
                    SELECT jsonb_agg(to_jsonb(a) - 'owner_id' ORDER BY a.created_at, a.id)
                    FROM accounts a WHERE a.owner_id = u.id
                ), '[]'::jsonb),
                'transactions', COALESCE((
                    SELECT jsonb_agg(to_jsonb(t) ORDER BY t.occurred_on, t.created_at, t.id)
                    FROM transactions t
                    JOIN accounts a ON a.id = t.account_id
                    WHERE a.owner_id = u.id
                ), '[]'::jsonb),
                'transfers', COALESCE((
                    SELECT jsonb_agg(to_jsonb(tr) - 'user_id' ORDER BY tr.created_at, tr.id)
                    FROM transfers tr WHERE tr.user_id = u.id
                ), '[]'::jsonb),
                'cards', COALESCE((
                    SELECT jsonb_agg(to_jsonb(c) - 'owner_id' ORDER BY c.created_at, c.id)
                    FROM cards c WHERE c.owner_id = u.id
                ), '[]'::jsonb),
                'invoices', COALESCE((
                    SELECT jsonb_agg(to_jsonb(i) ORDER BY i.created_at, i.id)
                    FROM invoices i JOIN cards c ON c.id = i.card_id
                    WHERE c.owner_id = u.id
                ), '[]'::jsonb),
                'invoicePaymentTransactions', COALESCE((
                    SELECT jsonb_agg(to_jsonb(ipt) ORDER BY ipt.invoice_id, ipt.transaction_id)
                    FROM invoice_payment_transactions ipt
                    JOIN invoices i ON i.id = ipt.invoice_id
                    JOIN cards c ON c.id = i.card_id
                    WHERE c.owner_id = u.id
                ), '[]'::jsonb),
                'installments', COALESCE((
                    SELECT jsonb_agg(to_jsonb(inst) - 'user_id' ORDER BY inst.created_at, inst.id)
                    FROM installments inst WHERE inst.user_id = u.id
                ), '[]'::jsonb),
                'budgets', COALESCE((
                    SELECT jsonb_agg(to_jsonb(b) - 'user_id' ORDER BY b.created_at, b.id)
                    FROM budgets b WHERE b.user_id = u.id
                ), '[]'::jsonb),
                'goals', COALESCE((
                    SELECT jsonb_agg(to_jsonb(g) - 'user_id' ORDER BY g.created_at, g.id)
                    FROM goals g WHERE g.user_id = u.id
                ), '[]'::jsonb),
                'goalContributions', COALESCE((
                    SELECT jsonb_agg(to_jsonb(gc) - 'user_id' ORDER BY gc.created_at, gc.id)
                    FROM goal_contributions gc WHERE gc.user_id = u.id
                ), '[]'::jsonb),
                'recurrences', COALESCE((
                    SELECT jsonb_agg(to_jsonb(r) - 'user_id' ORDER BY r.created_at, r.id)
                    FROM recurrences r WHERE r.user_id = u.id
                ), '[]'::jsonb),
                'categories', COALESCE((
                    SELECT jsonb_agg(to_jsonb(cat) - 'owner_id' ORDER BY cat.created_at, cat.id)
                    FROM categories cat WHERE cat.owner_id = u.id
                ), '[]'::jsonb),
                'notifications', COALESCE((
                    SELECT jsonb_agg(to_jsonb(n) - 'user_id' ORDER BY n.created_at, n.id)
                    FROM notifications n WHERE n.user_id = u.id
                ), '[]'::jsonb)
            )::text AS export
            FROM users u
            WHERE u.id = ?
        """
    }
}
