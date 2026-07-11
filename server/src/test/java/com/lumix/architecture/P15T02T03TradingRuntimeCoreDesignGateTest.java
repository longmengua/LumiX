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
 * 驗證 Phase 15-T02 與 P15-T03 只建立 design gate，不會偷接成正式 runtime。
 */
class P15T02T03TradingRuntimeCoreDesignGateTest {

    /**
     * 確認 trading 相關 source 仍沒有正式 posting / balance runtime token 與 runtime SQL。
     *
     * 這個 case 必須存在，因為 P15-T02 / T03 若越界，就會把 design gate 直接變成正式 money movement。
     */
    @Test
    void designSourcesDoNotContainRuntimeTokensOrSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> sourceFiles = List.of(
                "com/lumix/trading/core/TradingRuntimeCoreSafetyPolicy.java",
                "com/lumix/trading/core/TradingRuntimeCoreSafetyContract.java",
                "com/lumix/trading/core/TradingRuntimeCoreScope.java",
                "com/lumix/trading/core/posting/LedgerPostingIntegrationDesign.java",
                "com/lumix/trading/core/posting/LedgerPostingIntegrationPolicy.java",
                "com/lumix/trading/core/posting/LedgerPostingIntegrationStep.java",
                "com/lumix/trading/core/projection/BalanceProjectionRuntimeDesign.java",
                "com/lumix/trading/core/projection/BalanceProjectionRuntimePolicy.java",
                "com/lumix/trading/core/projection/BalanceProjectionCapability.java",
                "com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java",
                "com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java",
                "com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java"
        );

        List<String> forbiddenTokens = List.of(
                "LedgerPostingService",
                "BalanceProjectionService",
                "BalanceMutationService",
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
                "INSERT INTO reservations",
                "UPDATE reservations",
                "DELETE FROM reservations",
                "INSERT INTO settlements",
                "UPDATE settlements",
                "DELETE FROM settlements"
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
     * 確認 posting boundary 仍沒有直接引用 append adapter。
     *
     * 這個 case 必須存在，因為 ledger posting integration 還停留在 design gate，不能跨層接到 JDBC adapter。
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
    }

    /**
     * 確認 Phase 15 的 task 文件已列出 T02 與 T03。
     *
     * 這個 case 必須存在，因為 design gate 需要明確的文件索引與任務序列。
     */
    @Test
    void phase15READMEAndTasksIncludeT02AndT03() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T02 ledger posting integration design gate"));
        assertTrue(readme.contains("T03 balance projection runtime design gate"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("ledger-posting-integration-design.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("balance-projection-runtime-design.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T02.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T03.md")));
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
