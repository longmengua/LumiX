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
 * 驗證 Phase 14-T05 只建立 ledger append transaction boundary design，不把 runtime 寫入能力帶進來。
 */
class P14T05LedgerAppendTransactionBoundaryTest {

    private static final List<String> TRANSACTION_FILES = List.of(
            "com/lumix/ledger/application/transaction/package-info.java",
            "com/lumix/ledger/application/transaction/LedgerAppendTransactionStep.java",
            "com/lumix/ledger/application/transaction/LedgerAppendTransactionDesign.java",
            "com/lumix/ledger/application/transaction/LedgerAppendTransactionBoundary.java",
            "com/lumix/ledger/application/transaction/LedgerAppendTransactionPolicy.java"
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
     * 確認 transaction package marker 都已就位。
     *
     * 如果這些檔案消失，後續 phase 就會失去 transaction boundary 的明確註記。
     */
    @Test
    void packageMarkersExist() {
        Path sourceRoot = resolveSourceRoot();
        Path packageInfo = sourceRoot.resolve("com/lumix/ledger/application/transaction/package-info.java");
        assertTrue(Files.isRegularFile(packageInfo), "Missing package marker: " + packageInfo);
    }

    /**
     * 確認 transaction design source 沒有 repository / transaction / write token。
     *
     * 這個限制讓 P14-T05 保持乾淨：先定義 transaction boundary design，不先接正式寫入路徑。
     */
    @Test
    void transactionBoundaryDoesNotContainWriteTokens() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Set<String> forbiddenTokens = Set.of(
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "insert(",
                "update(",
                "delete(",
                "balance_projections",
                "IdempotencyService",
                "OutboxPublisher",
                "AuditWriter"
        );

        for (String relativePath : TRANSACTION_FILES) {
            Path sourceFile = sourceRoot.resolve(relativePath);
            String source = Files.readString(sourceFile);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken),
                        "Forbidden token found in transaction design: " + forbiddenToken + " @ " + sourceFile);
            }
        }
    }
}
