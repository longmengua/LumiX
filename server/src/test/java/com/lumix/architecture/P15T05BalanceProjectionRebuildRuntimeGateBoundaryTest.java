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
 * 驗證 Phase 15-T05 只建立 balance projection rebuild gate，不會偷跑成完整 balance mutation runtime。
 */
class P15T05BalanceProjectionRebuildRuntimeGateBoundaryTest {

    /**
     * 確認 rebuild gate source 沒有正式 runtime token、repository/transaction annotation 或 ledger mutation SQL。
     *
     * 這個 case 必須存在，因為 P15-T05 一旦越界，就會把 read model rebuild 擴張成正式 balance mutation path。
     */
    @Test
    void rebuildGateSourcesDoNotContainForbiddenRuntimeTokensOrLedgerMutationSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> sourceFiles = List.of(
                "com/lumix/trading/core/projection/BalanceProjectionRuntimePolicy.java",
                "com/lumix/trading/core/projection/runtime/package-info.java",
                "com/lumix/trading/core/projection/runtime/BalanceProjectionRebuildResult.java",
                "com/lumix/trading/core/projection/runtime/BalanceProjectionRebuildGate.java"
        );

        List<String> forbiddenTokens = List.of(
                "BalanceMutationService",
                "LedgerPostingService",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager"
        );
        List<String> forbiddenSqlSnippets = List.of(
                "INSERT INTO ledger_journals",
                "UPDATE ledger_journals",
                "DELETE FROM ledger_journals",
                "INSERT INTO ledger_entries",
                "UPDATE ledger_entries",
                "DELETE FROM ledger_entries",
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
     * 確認 Phase 15 README 與 task 索引已列出 rebuild gate。
     *
     * 這個 case 必須存在，因為文件索引就是 phase gate 的施工入口。
     */
    @Test
    void phase15READMEAndTaskIndexIncludeT05() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T05 balance projection rebuild runtime gate"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("balance-projection-rebuild-gate.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T05.md")));
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
