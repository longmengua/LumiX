package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 P14-T08 只建立 idempotency / request identity 設計門檻，不會偷接成正式 runtime。
 */
class P14T08LedgerIdempotencyDesignGateTest {

    /**
     * 確認新設計檔與相關 boundary source 不含 DB client、runtime service 或誤接 adapter 的痕跡。
     *
     * 這個 case 必須存在，因為 idempotency 一旦接到 runtime，會直接影響重送與重複執行風險。
     */
    @Test
    void designSourcesDoNotContainRuntimeOrDatabaseTokens() throws IOException {
        List<Path> files = List.of(
                Path.of("src/main/java/com/lumix/ledger/application/idempotency/package-info.java"),
                Path.of("src/main/java/com/lumix/ledger/application/idempotency/LedgerIdempotencyScope.java"),
                Path.of("src/main/java/com/lumix/ledger/application/idempotency/LedgerIdempotencyDecision.java"),
                Path.of("src/main/java/com/lumix/ledger/application/idempotency/LedgerRequestIdentityContract.java"),
                Path.of("src/main/java/com/lumix/ledger/application/idempotency/LedgerIdempotencyDesign.java"),
                Path.of("src/main/java/com/lumix/ledger/application/idempotency/LedgerIdempotencyDesignPolicy.java"),
                Path.of("src/main/java/com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java"),
                Path.of("src/main/java/com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java"),
                Path.of("src/main/java/com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java")
        );

        List<String> forbiddenTokens = List.of(
                "IdempotencyService",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager"
        );

        List<String> forbiddenSqlSnippets = List.of(
                "INSERT INTO idempotency_keys",
                "UPDATE idempotency_keys",
                "DELETE FROM idempotency_keys",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "INSERT INTO audit_logs",
                "UPDATE audit_logs",
                "DELETE FROM audit_logs"
        );

        for (Path file : files) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken), () -> file + " contains forbidden token: " + forbiddenToken);
            }
            for (String forbiddenSqlSnippet : forbiddenSqlSnippets) {
                assertFalse(source.contains(forbiddenSqlSnippet), () -> file + " contains forbidden SQL: " + forbiddenSqlSnippet);
            }
        }
    }

    /**
     * 確認 posting boundary source 仍沒有接到 LedgerAppendOnlyJdbcAdapter。
     *
     * 這個 case 必須存在，因為 command boundary 只能產生 plan，不能直接跨成 DB 寫入路徑。
     */
    @Test
    void postingBoundaryStillDoesNotReferenceJdbcAdapter() throws IOException {
        String commandBoundarySource = Files.readString(
                Path.of("src/main/java/com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java"),
                StandardCharsets.UTF_8
        );
        String defaultBoundarySource = Files.readString(
                Path.of("src/main/java/com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(commandBoundarySource.contains("LedgerAppendOnlyJdbcAdapter"));
        assertFalse(defaultBoundarySource.contains("LedgerAppendOnlyJdbcAdapter"));
    }
}
