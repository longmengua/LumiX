package com.lumix.account.history;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcAssetLedgerHistoryQueryRepositoryTest {
    /** SQL 必須在 accounts.user_id 做 owner 過濾，並以 journal time 和 entry id 共同 keyset，不可寫入 ledger。 */
    @Test
    void scopesAndPaginatesTheImmutableLedgerRead() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcAssetLedgerHistoryQueryRepository repository = new JdbcAssetLedgerHistoryQueryRepository(jdbcTemplate);
        repository.findByOwnerUserId("owner", Optional.of(new AssetLedgerHistoryCursor(Instant.parse("2026-09-15T00:00:00Z"), 18)), 26);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), org.mockito.ArgumentMatchers.<RowMapper<AssetLedgerHistoryItem>>any(),
            org.mockito.ArgumentMatchers.eq("owner"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(18L),
            org.mockito.ArgumentMatchers.eq(26));
        assertTrue(sql.getValue().contains("FROM ledger_entries entry"));
        assertTrue(sql.getValue().contains("WHERE account.user_id = ?"));
        assertTrue(sql.getValue().contains("(journal.posted_at, entry.ledger_entry_id) < (?, ?)"));
        assertTrue(sql.getValue().contains("ORDER BY journal.posted_at DESC, entry.ledger_entry_id DESC"));
        assertFalse(sql.getValue().matches("(?s).*\\b(INSERT|UPDATE|DELETE)\\b.*"));
    }
}
