package com.lumix.admin.asset;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL 管理端歷史唯讀 adapter；owner 關聯只用於查詢範圍，SQL 永不修改 ledger。 */
@Repository @Profile("infrastructure")
class JdbcAdminUserLedgerHistoryQueryRepository implements AdminUserLedgerHistoryQueryRepository {
    private final JdbcTemplate jdbcTemplate;
    JdbcAdminUserLedgerHistoryQueryRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override public List<AdminUserLedgerHistoryItem> findLatestByUserId(String userId, int limit) {
        return jdbcTemplate.query("""
            SELECT entry.ledger_entry_id, account.account_type, entry.asset_symbol, entry.direction, entry.amount,
                   journal.business_reference_type, journal.business_reference_id, journal.posted_at
            FROM ledger_entries entry JOIN ledger_journals journal ON journal.ledger_journal_id = entry.ledger_journal_id
            JOIN accounts account ON account.account_id = entry.account_id WHERE account.user_id = ?
            ORDER BY journal.posted_at DESC, entry.ledger_entry_id DESC LIMIT ?
            """, (rs, row) -> new AdminUserLedgerHistoryItem(rs.getLong(1), AccountType.valueOf(rs.getString(2)), new AssetSymbol(rs.getString(3)),
            rs.getString(4), new MoneyAmount(rs.getBigDecimal(5)), rs.getString(6), rs.getString(7), rs.getTimestamp(8).toInstant()), userId, limit);
    }
}
