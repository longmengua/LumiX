package com.lumix.account.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.lumix.testing.JdbcDataSource;

/** 驗證 owner predicate 與 keyset cursor 對齊 Flyway 建立的真實 PostgreSQL ledger schema。 */
class JdbcAssetLedgerHistoryQueryRepositoryIntegrationTest {
    /** 對方帳戶 entry 不可混入，且同一 postedAt 時仍依 entryId 維持穩定、可續讀的順序。 */
    @Test
    void readsOnlyOwnerEntriesAndContinuesWithKeysetCursor() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:postgresql:test:asset_t03_history_query"); source.setUser("ignored"); source.setPassword("ignored");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        seed(source);
        JdbcAssetLedgerHistoryQueryRepository repository = new JdbcAssetLedgerHistoryQueryRepository(new JdbcTemplate(source));

        List<AssetLedgerHistoryItem> first = repository.findByOwnerUserId("owner", Optional.empty(), 2);
        List<AssetLedgerHistoryItem> second = repository.findByOwnerUserId("owner",
            Optional.of(new AssetLedgerHistoryCursor(first.getLast().postedAt(), first.getLast().entryId())), 2);

        assertEquals(List.of("owner-new", "owner-same-time"), first.stream().map(AssetLedgerHistoryItem::referenceId).toList());
        assertEquals(List.of("owner-old"), second.stream().map(AssetLedgerHistoryItem::referenceId).toList());
        assertTrue(first.stream().noneMatch(item -> item.referenceId().equals("other-owner")));
        assertEquals(new BigDecimal("123456789012345678.123456789012345678"), first.getFirst().amount().value());
    }

    /** 測試資料僅存在隔離的 PostgreSQL test schema；它是 adapter 證據，不能成為正式 API fallback。 */
    private static void seed(JdbcDataSource source) throws Exception {
        try (Connection connection = source.getConnection()) {
            exec(connection, "INSERT INTO users (user_id,email,display_name,status) VALUES ('owner','owner@example.com','Owner','ACTIVE'),('other','other@example.com','Other','ACTIVE')");
            exec(connection, "INSERT INTO assets (asset_symbol,display_name,precision_scale,status) VALUES ('USDT','Tether',18,'ACTIVE')");
            exec(connection, "INSERT INTO accounts (account_id,user_id,account_type,status) VALUES ('owner-spot','owner','SPOT','ACTIVE'),('other-spot','other','SPOT','ACTIVE')");
            journal(connection, "owner-old", "2026-09-13T00:00:00Z"); entry(connection, "owner-spot", 1, "1.000000000000000000");
            journal(connection, "owner-same-time", "2026-09-14T00:00:00Z"); entry(connection, "owner-spot", 2, "2.000000000000000000");
            journal(connection, "other-owner", "2026-09-15T00:00:00Z"); entry(connection, "other-spot", 3, "3.000000000000000000");
            journal(connection, "owner-new", "2026-09-15T00:00:00Z"); entry(connection, "owner-spot", 4, "123456789012345678.123456789012345678");
        }
    }

    private static void journal(Connection connection, String reference, String postedAt) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO ledger_journals (business_reference_type,business_reference_id,posted_at) VALUES ('TRADE',?,?)")) {
            statement.setString(1, reference); statement.setObject(2, OffsetDateTime.ofInstant(Instant.parse(postedAt), ZoneOffset.UTC)); statement.executeUpdate();
        }
    }
    private static void entry(Connection connection, String accountId, int sequence, String amount) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO ledger_entries (ledger_journal_id,entry_sequence,account_id,asset_symbol,direction,amount) VALUES (?,?,?,'USDT','CREDIT',?)")) {
            statement.setLong(1, sequence); statement.setInt(2, 1); statement.setString(3, accountId); statement.setBigDecimal(4, new BigDecimal(amount)); statement.executeUpdate();
        }
    }
    private static void exec(Connection connection, String sql) throws Exception { try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.executeUpdate(); } }
}
