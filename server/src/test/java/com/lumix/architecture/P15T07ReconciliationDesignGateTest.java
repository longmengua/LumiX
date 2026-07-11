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
 * 驗證 Phase 15-T07 只建立 reconciliation design gate，不會偷接成正式 reconciliation runtime。
 */
class P15T07ReconciliationDesignGateTest {

    /**
     * 確認 reconciliation design sources 沒有正式 runtime token、repository/transaction annotation 或寫回型 SQL。
     *
     * 這個 case 必須存在，因為 reconciliation 一旦變成自動修正，就會直接碰到資金風險。
     */
    @Test
    void reconciliationDesignSourcesDoNotContainForbiddenRuntimeTokensOrWriteSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> sourceFiles = List.of(
                "com/lumix/trading/core/package-info.java",
                "com/lumix/trading/core/TradingRuntimeCoreScope.java",
                "com/lumix/trading/core/TradingRuntimeCoreSafetyContract.java",
                "com/lumix/trading/core/TradingRuntimeCoreSafetyPolicy.java",
                "com/lumix/trading/core/reconciliation/package-info.java",
                "com/lumix/trading/core/reconciliation/ReconciliationDesignDecision.java",
                "com/lumix/trading/core/reconciliation/ReconciliationSignalType.java",
                "com/lumix/trading/core/reconciliation/ReconciliationDesign.java",
                "com/lumix/trading/core/reconciliation/ReconciliationDesignPolicy.java"
        );

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
     * 確認 Phase 15 README 與 task 索引已列出 reconciliation design gate。
     *
     * 這個 case 必須存在，因為 design gate 的文件索引就是後續施工與審核的入口。
     */
    @Test
    void phase15READMEAndTaskIndexIncludeT07() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T07 reconciliation design gate"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("reconciliation-design.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T07.md")));
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
