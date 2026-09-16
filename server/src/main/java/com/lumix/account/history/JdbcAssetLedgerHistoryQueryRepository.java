package com.lumix.account.history;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL immutable ledger history 唯讀 adapter。
 *
 * <p>owner predicate 必須在 accounts join 上成立，且 cursor 比較同時使用 posted_at 與 entry id。SQL 沒有任何
 * mutation statement；journal 的其他 entry 不會因同一 journal 而洩漏至目前 owner。</p>
 */
@Repository
@Profile("infrastructure")
class JdbcAssetLedgerHistoryQueryRepository implements AssetLedgerHistoryQueryRepository {
    private static final String BASE_SQL = """
        SELECT entry.ledger_entry_id,
               journal.ledger_journal_id,
               account.account_type,
               entry.asset_symbol,
               entry.direction,
               entry.amount,
               journal.business_reference_type,
               journal.business_reference_id,
               journal.posted_at,
               entry.created_at
        FROM ledger_entries entry
        JOIN ledger_journals journal ON journal.ledger_journal_id = entry.ledger_journal_id
        JOIN accounts account ON account.account_id = entry.account_id
        WHERE account.user_id = ?
        """;
    private static final String ORDER_AND_LIMIT = " ORDER BY journal.posted_at DESC, entry.ledger_entry_id DESC LIMIT ?";
    private final JdbcTemplate jdbcTemplate;

    JdbcAssetLedgerHistoryQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AssetLedgerHistoryItem> findByOwnerUserId(String userId, Optional<AssetLedgerHistoryCursor> before, int limit) {
        String sql = BASE_SQL + before.map(ignored -> " AND (journal.posted_at, entry.ledger_entry_id) < (?, ?)").orElse("") + ORDER_AND_LIMIT;
        Object[] parameters = before
            .<Object[]>map(cursor -> new Object[] { userId, Timestamp.from(cursor.postedAt()), cursor.entryId(), limit })
            .orElseGet(() -> new Object[] { userId, limit });
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new AssetLedgerHistoryItem(
            resultSet.getLong("ledger_entry_id"), resultSet.getLong("ledger_journal_id"),
            AccountType.valueOf(resultSet.getString("account_type")), new AssetSymbol(resultSet.getString("asset_symbol")),
            resultSet.getString("direction"), new MoneyAmount(resultSet.getBigDecimal("amount")),
            resultSet.getString("business_reference_type"), resultSet.getString("business_reference_id"),
            toInstant(resultSet.getTimestamp("posted_at")), toInstant(resultSet.getTimestamp("created_at"))
        ), parameters);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
