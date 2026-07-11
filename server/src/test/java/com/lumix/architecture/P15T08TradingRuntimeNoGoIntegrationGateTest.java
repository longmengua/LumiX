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
 * 驗證 Phase 15-T08 只建立 trading runtime no-go integration gate，不會偷接成正式 runtime。
 */
class P15T08TradingRuntimeNoGoIntegrationGateTest {

    /**
     * 確認 main source 沒有新的 trading runtime service token 或不該新增的寫入 SQL。
     *
     * 這個 case 必須存在，因為 no-go gate 的目的就是鎖住 order / matching / settlement / futures / liquidation / withdrawal / security runtime。
     */
    @Test
    void mainSourcesDoNotContainForbiddenRuntimeTokensOrWriteSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<Path> sourceFiles;
        try (var files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }

        List<String> forbiddenTokens = List.of(
                "ReconciliationService",
                "ReservationRuntimeService",
                "ReservationService",
                "SettlementService",
                "MatchingService",
                "OrderPlacementService",
                "FuturesService",
                "LiquidationService",
                "WithdrawalService",
                "BalanceMutationService",
                "BalanceProjectionService",
                "LedgerPostingService",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager"
        );
        List<String> forbiddenSqlSnippets = List.of(
                "INSERT INTO reservations",
                "UPDATE reservations",
                "DELETE FROM reservations",
                "INSERT INTO balance_projections",
                "UPDATE balance_projections",
                "DELETE FROM balance_projections",
                "INSERT INTO ledger_journals",
                "UPDATE ledger_journals",
                "DELETE FROM ledger_journals",
                "INSERT INTO ledger_entries",
                "UPDATE ledger_entries",
                "DELETE FROM ledger_entries",
                "INSERT INTO idempotency_keys",
                "UPDATE idempotency_keys",
                "DELETE FROM idempotency_keys",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "INSERT INTO audit_logs",
                "UPDATE audit_logs",
                "DELETE FROM audit_logs",
                "INSERT INTO orders",
                "UPDATE orders",
                "DELETE FROM orders",
                "INSERT INTO trades",
                "UPDATE trades",
                "DELETE FROM trades"
        );

        List<Path> approvedWritePaths = List.of(
                sourceRoot.resolve("com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java"),
                sourceRoot.resolve("com/lumix/trading/core/projection/runtime/BalanceProjectionRebuildGate.java")
        );

        for (Path javaFile : sourceFiles) {
            if (approvedWritePaths.contains(javaFile)) {
                continue;
            }

            String source = Files.readString(javaFile, StandardCharsets.UTF_8);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken),
                        () -> javaFile + " contains forbidden token: " + forbiddenToken);
            }
            for (String forbiddenSqlSnippet : forbiddenSqlSnippets) {
                assertFalse(source.contains(forbiddenSqlSnippet),
                        () -> javaFile + " contains forbidden SQL: " + forbiddenSqlSnippet);
            }
        }
    }

    /**
     * 確認 Phase 15 no-go 文件已列出。
     *
     * 這個 case 必須存在，因為 no-go gate 不只是程式掃描，也要有可讀的文件入口。
     */
    @Test
    void phase15READMEAndTaskIndexIncludeT08() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T08 trading runtime no-go integration gate"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("trading-runtime-no-go-integration.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T08.md")));
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

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_15_BALANCE_RECON");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("../docs/phases/PHASE_15_BALANCE_RECON");
    }
}
