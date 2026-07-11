package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 14-T06 只建立最小 append persistence adapter gate，不把正式 posting runtime 接進來。
 */
class P14T06LedgerAppendPersistenceAdapterTest {

    private static final String ADAPTER_SOURCE = "com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java";

    private static final List<String> POSTING_FILES = List.of(
            "com/lumix/ledger/application/posting/package-info.java",
            "com/lumix/ledger/application/posting/LedgerPostingCommand.java",
            "com/lumix/ledger/application/posting/LedgerPostingDecision.java",
            "com/lumix/ledger/application/posting/LedgerPostingRejection.java",
            "com/lumix/ledger/application/posting/LedgerPostingPlan.java",
            "com/lumix/ledger/application/posting/LedgerPostingCommandResult.java",
            "com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java",
            "com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java"
    );

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     *
     * 這樣驗證路徑不會綁死在某一種啟動方式，避免架構測試本身變成脆弱的環境假設。
     */
    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java");
    }

    /**
     * 確認 adapter package marker 都已就位。
     *
     * 如果這些檔案消失，後續 phase 就會失去 append adapter gate 的明確註記。
     */
    @Test
    void packageMarkersExist() {
        Path sourceRoot = resolveSourceRoot();
        Path packageInfo = sourceRoot.resolve("com/lumix/ledger/persistence/adapter/package-info.java");
        assertTrue(Files.isRegularFile(packageInfo), "Missing package marker: " + packageInfo);
    }

    /**
     * 確認 adapter source 只含有 append-only gate，不含 update / delete / balance mutation token。
     *
     * 這個限制讓 P14-T06 保持乾淨：只測最小 append gate，不把它誤解成完整 posting runtime。
     */
    @Test
    void adapterSourceOnlyContainsAppendOnlyTokens() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Set<String> forbiddenTokens = Set.of(
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "LedgerPostingService",
                "LedgerPostingCommandBoundary",
                "balance_projections",
                "UPDATE ledger_journals",
                "UPDATE ledger_entries",
                "DELETE FROM ledger_journals",
                "DELETE FROM ledger_entries"
        );

        Path sourceFile = sourceRoot.resolve(ADAPTER_SOURCE);
        String source = Files.readString(sourceFile);
        assertTrue(source.contains("INSERT INTO ledger_journals"),
                "Missing journal insert SQL in append adapter: " + sourceFile);
        assertTrue(source.contains("INSERT INTO ledger_entries"),
                "Missing entry insert SQL in append adapter: " + sourceFile);
        for (String forbiddenToken : forbiddenTokens) {
            assertFalse(source.contains(forbiddenToken),
                    "Forbidden token found in append adapter: " + forbiddenToken + " @ " + sourceFile);
        }
    }

    /**
     * 確認 posting command boundary 沒有直接接到 adapter 或 database client。
     *
     * 這個 case 直接保護 application boundary 與 persistence adapter 的分層。
     */
    @Test
    void postingBoundaryDoesNotReferenceJdbcAdapter() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Set<String> forbiddenTokens = Set.of(
                "LedgerAppendOnlyJdbcAdapter",
                "DataSource",
                "Connection",
                "PreparedStatement",
                "JdbcTemplate",
                "EntityManager",
                "@Repository",
                "@Transactional",
                "LedgerPostingService",
                "balance_projections"
        );

        for (String relativePath : POSTING_FILES) {
            Path sourceFile = sourceRoot.resolve(relativePath);
            String source = Files.readString(sourceFile);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken),
                        "Forbidden token found in posting boundary: " + forbiddenToken + " @ " + sourceFile);
            }
        }
    }
}
