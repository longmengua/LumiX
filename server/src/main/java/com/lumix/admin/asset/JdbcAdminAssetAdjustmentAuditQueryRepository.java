package com.lumix.admin.asset;

import com.lumix.account.AccountType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL 唯讀 adapter，將管理操作證據與 immutable 雙分錄交叉核對。 */
@Repository
@Profile("infrastructure")
class JdbcAdminAssetAdjustmentAuditQueryRepository implements AdminAssetAdjustmentAuditQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcAdminAssetAdjustmentAuditQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AdminAssetAdjustmentAuditItem> findLatest(int limit) {
        return jdbcTemplate.query("""
                SELECT COALESCE(audit.audit_log_id, adjustment.adjustment_id) AS audit_log_id,
                       adjustment.ledger_journal_id, adjustment.actor_id, adjustment.user_id, target_user.email,
                       adjustment.account_type, adjustment.asset_symbol, adjustment.direction, adjustment.amount,
                       adjustment.adjustment_type AS activity_type, adjustment.reason,
                       CASE WHEN COUNT(entry.ledger_entry_id) = 2 AND COUNT(DISTINCT entry.asset_symbol) = 1
                                  AND SUM(CASE WHEN entry.direction = 'CREDIT' THEN entry.amount ELSE -entry.amount END) = 0
                            THEN 'VERIFIED' ELSE 'EXCEPTION' END AS reconciliation_status,
                       adjustment.created_at
                FROM admin_asset_adjustments adjustment
                JOIN users target_user ON target_user.user_id = adjustment.user_id
                LEFT JOIN audit_logs audit ON audit.target_type = 'ADMIN_ASSET_ADJUSTMENT' AND audit.target_id = adjustment.adjustment_id::TEXT
                LEFT JOIN ledger_entries entry ON entry.ledger_journal_id = adjustment.ledger_journal_id
                GROUP BY audit.audit_log_id, adjustment.adjustment_id, adjustment.ledger_journal_id, adjustment.actor_id,
                         adjustment.user_id, target_user.email, adjustment.account_type, adjustment.asset_symbol,
                         adjustment.direction, adjustment.amount, adjustment.adjustment_type, adjustment.reason,
                         adjustment.created_at
                ORDER BY adjustment.created_at DESC, adjustment.adjustment_id DESC
                LIMIT ?
                """, (resultSet, rowNumber) -> {
            return new AdminAssetAdjustmentAuditItem(
                    resultSet.getLong("audit_log_id"),
                    resultSet.getLong("ledger_journal_id"),
                    resultSet.getString("actor_id"),
                    resultSet.getString("user_id"),
                    resultSet.getString("email"),
                    AccountType.valueOf(resultSet.getString("account_type")),
                    resultSet.getString("target_asset_symbol"),
                    resultSet.getString("target_direction"),
                    resultSet.getBigDecimal("target_amount") == null ? null : resultSet.getBigDecimal("target_amount").toPlainString(),
                    resultSet.getString("activity_type"),
                    resultSet.getString("reason"),
                    resultSet.getString("reconciliation_status"),
                    resultSet.getTimestamp("created_at").toInstant()
            );
        }, limit);
    }

}
