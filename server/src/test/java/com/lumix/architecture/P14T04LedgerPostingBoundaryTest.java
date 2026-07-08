package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 14-T04 只建立 application command boundary，不把正式 posting runtime 帶進來。
 */
class P14T04LedgerPostingBoundaryTest {

    private static final List<String> POSTING_FILES = List.of(
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
     * 確認 posting application package marker 都已就位。
     *
     * 如果這些檔案消失，後續 phase 就會失去 application command boundary 的明確註記。
     */
    @Test
    void packageMarkersExist() {
        Path sourceRoot = resolveSourceRoot();
        Path packageInfo = sourceRoot.resolve("com/lumix/ledger/application/posting/package-info.java");
        assertTrue(Files.isRegularFile(packageInfo), "Missing package marker: " + packageInfo);
    }

    /**
     * 確認 command boundary source 沒有 repository / transaction / write token。
     *
     * 這個限制讓 P14-T04 保持乾淨：先定義 application boundary，不先接正式寫入路徑。
     */
    @Test
    void commandBoundaryDoesNotContainWriteTokens() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Set<String> forbiddenTokens = Set.of(
                "LedgerPostingService",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "insert(",
                "update(",
                "delete(",
                "balance_projections",
                "posted",
                "committed",
                "persisted"
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
