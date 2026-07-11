package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 15-T04 只建立受控 runtime gate，不會偷接成完整 trading runtime。
 */
class P15T04LedgerPostingRuntimeGateBoundaryTest {

    /**
     * 確認受控 runtime gate source 只保留 append gate，沒有正式 runtime token 或 forbidden SQL。
     *
     * 這個 case 必須存在，因為 P15-T04 若誤接成其他 runtime，就會把受控接線擴張成 production path。
     */
    @Test
    void runtimeGateSourceDoesNotContainForbiddenTokensOrSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> sourceFiles = List.of(
                "com/lumix/ledger/application/posting/runtime/package-info.java",
                "com/lumix/ledger/application/posting/runtime/LedgerPostingAppendResult.java",
                "com/lumix/ledger/application/posting/runtime/LedgerPostingRuntimeGate.java",
                "com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java",
                "com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java"
        );

        List<String> forbiddenTokens = List.of(
                "LedgerPostingService",
                "BalanceMutationService",
                "BalanceProjectionService",
                "ReservationRuntimeService",
                "SettlementService",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager"
        );
        List<String> forbiddenSqlSnippets = List.of(
                "INSERT INTO balance_projections",
                "UPDATE balance_projections",
                "DELETE FROM balance_projections",
                "INSERT INTO idempotency_keys",
                "UPDATE idempotency_keys",
                "DELETE FROM idempotency_keys",
                "SELECT * FROM idempotency_keys",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "INSERT INTO audit_logs",
                "UPDATE audit_logs",
                "DELETE FROM audit_logs"
        );

        for (String relativePath : sourceFiles) {
            Path sourceFile = sourceRoot.resolve(relativePath);
            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken),
                        () -> sourceFile + " contains forbidden token: " + forbiddenToken);
            }
            for (String forbiddenSqlSnippet : forbiddenSqlSnippets) {
                assertFalse(source.contains(forbiddenSqlSnippet),
                        () -> sourceFile + " contains forbidden SQL: " + forbiddenSqlSnippet);
            }
        }
    }

    /**
     * 確認 posting boundary 仍然沒有直接引用 append adapter。
     *
     * 這個 case 必須存在，因為正式接線必須由受控 runtime gate 完成，不能把 application boundary 直接黏到 JDBC adapter。
     */
    @Test
    void postingBoundaryStillDoesNotReferenceAppendAdapter() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Path commandBoundary = sourceRoot.resolve("com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java");
        Path defaultBoundary = sourceRoot.resolve("com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java");

        String commandBoundarySource = Files.readString(commandBoundary, StandardCharsets.UTF_8);
        String defaultBoundarySource = Files.readString(defaultBoundary, StandardCharsets.UTF_8);

        assertFalse(commandBoundarySource.contains("LedgerAppendOnlyJdbcAdapter"));
        assertFalse(defaultBoundarySource.contains("LedgerAppendOnlyJdbcAdapter"));
        assertTrue(Files.isRegularFile(sourceRoot.resolve("com/lumix/ledger/application/posting/runtime/LedgerPostingRuntimeGate.java")));
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java");
    }
}
