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
                SELECT audit.audit_log_id, journal.ledger_journal_id, audit.actor_id,
                       target_user.user_id, target_user.email, target_account.account_type,
                       MAX(entry.asset_symbol) FILTER (WHERE entry.account_id = audit.target_id) AS target_asset_symbol,
                       MAX(entry.direction) FILTER (WHERE entry.account_id = audit.target_id) AS target_direction,
                       MAX(entry.amount) FILTER (WHERE entry.account_id = audit.target_id) AS target_amount,
                       audit.reason,
                       CASE WHEN COUNT(entry.ledger_entry_id) = 2
                                  AND COUNT(DISTINCT entry.asset_symbol) = 1
                                  AND SUM(CASE WHEN entry.direction = 'CREDIT' THEN entry.amount ELSE -entry.amount END) = 0
                                  AND COUNT(*) FILTER (WHERE entry.direction = 'CREDIT') = 1
                                  AND COUNT(*) FILTER (WHERE entry.direction = 'DEBIT') = 1
                                  AND COUNT(*) FILTER (WHERE entry.account_id = audit.target_id) = 1
                            THEN 'VERIFIED' ELSE 'EXCEPTION' END AS reconciliation_status,
                       journal.posted_at
                FROM audit_logs audit
                JOIN ledger_journals journal ON journal.request_id = audit.request_id
                JOIN accounts target_account ON target_account.account_id = audit.target_id
                JOIN users target_user ON target_user.user_id = target_account.user_id
                LEFT JOIN ledger_entries entry ON entry.ledger_journal_id = journal.ledger_journal_id
                WHERE audit.action_type = 'ADMIN_ASSET_ADJUSTMENT'
                  AND audit.outcome = 'SUCCESS'
                  AND audit.target_type = 'USER_ACCOUNT'
                  AND journal.business_reference_type = 'ADJUSTMENT'
                GROUP BY audit.audit_log_id, journal.ledger_journal_id, audit.actor_id,
                         target_user.user_id, target_user.email, target_account.account_type,
                         audit.target_id, audit.reason, journal.posted_at
                ORDER BY journal.posted_at DESC, journal.ledger_journal_id DESC
                LIMIT ?
                """, (resultSet, rowNumber) -> {
            AdjustmentReason reason = AdjustmentReason.parse(resultSet.getString("reason"));
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
                    reason.activityType(),
                    reason.note(),
                    resultSet.getString("reconciliation_status"),
                    resultSet.getTimestamp("posted_at").toInstant()
            );
        }, limit);
    }

    /**
     * 現有 immutable audit reason 以 key/value 寫入；此 parser 僅用來友善顯示既有證據，
     * 金額與資產一律仍以 ledger entries 為準，避免把自由文字當成帳務真相。
     */
    private record AdjustmentReason(String activityType, String note) {
        private static AdjustmentReason parse(String value) {
            if (value == null || value.isBlank()) {
                return new AdjustmentReason("UNKNOWN", "");
            }
            int noteIndex = value.indexOf(";reason=");
            String metadata = noteIndex < 0 ? value : value.substring(0, noteIndex);
            String note = noteIndex < 0 ? "" : value.substring(noteIndex + ";reason=".length());
            Map<String, String> values = new HashMap<>();
            for (String part : metadata.split(";")) {
                int separator = part.indexOf('=');
                if (separator > 0) values.put(part.substring(0, separator), part.substring(separator + 1));
            }
            String activityType = values.getOrDefault("type", "UNKNOWN");
            return new AdjustmentReason(
                    "AIRDROP".equals(activityType) || "REVERSAL".equals(activityType) ? activityType : "UNKNOWN",
                    note
            );
        }
    }
}
