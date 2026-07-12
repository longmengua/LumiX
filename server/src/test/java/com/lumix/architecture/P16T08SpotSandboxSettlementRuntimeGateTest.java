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
 * 驗證 Phase 16-T08 只建立 spot sandbox settlement runtime gate，不會偷接成正式 settlement runtime。
 */
class P16T08SpotSandboxSettlementRuntimeGateTest {

    /**
     * 確認 main source 只保存 settlement runtime gate 契約，不會出現正式 runtime token 或寫入 SQL。
     *
     * 這個 case 必須存在，因為 P16-T08 只能產生 sandbox plan candidate，不能偷渡成正式 settlement / ledger / balance runtime。
     */
    @Test
    void settlementRuntimeSourcesDoNotContainForbiddenRuntimeTokensOrWriteSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<Path> sourceFiles;
        try (var files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }

        List<String> forbiddenTokens = List.of(
                "SettlementService",
                "SettlementEngine",
                "TradeSettlementService",
                "MatchingService",
                "MatchingEngine",
                "TradeService",
                "ReservationRuntimeService",
                "ReservationService",
                "OrderPlacementService",
                "SpotTradingService",
                "LedgerPostingService",
                "BalanceMutationService",
                "BalanceProjectionService",
                "LedgerPostingRuntimeGate",
                "LedgerAppendOnlyJdbcAdapter",
                "BalanceProjectionRebuildGate",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "Connection",
                "PreparedStatement"
        );
        List<String> forbiddenSqlSnippets = List.of(
                "INSERT INTO orders",
                "UPDATE orders",
                "DELETE FROM orders",
                "SELECT * FROM orders",
                "INSERT INTO trades",
                "UPDATE trades",
                "DELETE FROM trades",
                "SELECT * FROM trades",
                "INSERT INTO reservations",
                "UPDATE reservations",
                "DELETE FROM reservations",
                "SELECT * FROM reservations",
                "INSERT INTO balance_projections",
                "UPDATE balance_projections",
                "DELETE FROM balance_projections",
                "SELECT * FROM balance_projections",
                "INSERT INTO ledger_journals",
                "UPDATE ledger_journals",
                "DELETE FROM ledger_journals",
                "SELECT * FROM ledger_journals",
                "INSERT INTO ledger_entries",
                "UPDATE ledger_entries",
                "DELETE FROM ledger_entries",
                "SELECT * FROM ledger_entries",
                "INSERT INTO idempotency_keys",
                "UPDATE idempotency_keys",
                "DELETE FROM idempotency_keys",
                "SELECT * FROM idempotency_keys",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "SELECT * FROM outbox_events",
                "INSERT INTO audit_logs",
                "UPDATE audit_logs",
                "DELETE FROM audit_logs",
                "SELECT * FROM audit_logs"
        );

        for (Path javaFile : sourceFiles) {
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
     * 確認 Phase 16 README 與 task 索引已列出 P16-T08。
     *
     * 這個 case 必須存在，因為 settlement runtime gate 的文件入口就是後續 sandbox 施工與審核的起點。
     */
    @Test
    void phase16READMEAndTaskIndexIncludeT08() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T08 sandbox settlement runtime gate"));
        assertTrue(readme.contains("P16-T08 completed as sandbox settlement runtime gate only"));
        assertTrue(readme.contains("settlement runtime not started"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P16-T08.md")));
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     *
     * 這裡只掃 spot settlement main source，避免把其他 phase 或其他 spot boundary 的既有契約誤判成 P16 settlement 停止線違規。
     */
    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java/com/lumix/trading/core/spot/settlement");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java/com/lumix/trading/core/spot/settlement");
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_16_SPOT_TRADING_SANDBOX");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("../docs/phases/PHASE_16_SPOT_TRADING_SANDBOX");
    }
}
