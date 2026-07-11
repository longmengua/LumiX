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
 * 驗證 Phase 14 的 runtime integration 仍然是 no-go gate，不會偷接成正式 runtime。
 */
class P14T09LedgerRuntimeIntegrationNoGoGateTest {

    /**
     * 確認 ledger 相關 source 仍沒有 runtime 接線與資料庫 mutation token。
     *
     * 這個 case 必須存在，因為一旦這些 token 出現，代表 ledger 可能已經從 skeleton 轉成正式執行路徑。
     */
    @Test
    void runtimeIntegrationNoGoTokensRemainAbsent() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> sourceFiles = List.of(
                "com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java",
                "com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java",
                "com/lumix/ledger/application/posting/LedgerPostingCommand.java",
                "com/lumix/ledger/application/posting/LedgerPostingPlan.java",
                "com/lumix/ledger/application/posting/LedgerPostingCommandResult.java",
                "com/lumix/ledger/application/posting/LedgerPostingRejection.java",
                "com/lumix/ledger/application/transaction/LedgerAppendTransactionPolicy.java",
                "com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java",
                "com/lumix/ledger/application/idempotency/LedgerIdempotencyDesignPolicy.java",
                "com/lumix/ledger/application/idempotency/LedgerIdempotencyDesign.java",
                "com/lumix/ledger/application/idempotency/LedgerRequestIdentityContract.java"
        );

        List<String> forbiddenTokens = List.of(
                "LedgerPostingService",
                "OutboxPublisher",
                "AuditWriter",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "INSERT INTO idempotency_keys",
                "UPDATE idempotency_keys",
                "DELETE FROM idempotency_keys",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "INSERT INTO audit_logs",
                "UPDATE audit_logs",
                "DELETE FROM audit_logs",
                "INSERT INTO balance_projections",
                "UPDATE balance_projections",
                "DELETE FROM balance_projections"
        );

        for (String relativePath : sourceFiles) {
            Path sourceFile = sourceRoot.resolve(relativePath);
            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken),
                        () -> sourceFile + " contains forbidden token: " + forbiddenToken);
            }
        }
    }

    /**
     * 確認 posting boundary 仍未直接引用 append adapter。
     *
     * 這個 case 保護的是 application boundary 與 persistence adapter 的分層，不讓 no-go gate 被誤破壞。
     */
    @Test
    void postingBoundaryStillDoesNotReferenceAppendAdapter() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> postingFiles = List.of(
                "com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java",
                "com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java",
                "com/lumix/ledger/application/posting/LedgerPostingCommand.java",
                "com/lumix/ledger/application/posting/LedgerPostingPlan.java",
                "com/lumix/ledger/application/posting/LedgerPostingCommandResult.java",
                "com/lumix/ledger/application/posting/LedgerPostingRejection.java"
        );

        for (String relativePath : postingFiles) {
            Path sourceFile = sourceRoot.resolve(relativePath);
            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            assertFalse(source.contains("LedgerAppendOnlyJdbcAdapter"),
                    () -> sourceFile + " must not reference LedgerAppendOnlyJdbcAdapter");
        }

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
