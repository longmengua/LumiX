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
 * 驗證 Phase 15-T01 只建立 Trading Runtime Core 的 scope gate 與 safety contracts，不會偷接正式 runtime。
 */
class P15T01TradingRuntimeCoreScopeGateTest {

    /**
     * 確認交易 runtime 仍沒有正式 money movement runtime、repository、transaction 或 database client token。
     *
     * 這個 case 必須存在，因為 P15 一旦越界，就會把 ledger / balance / reservation / settlement 直接推進正式交易路徑。
     */
    @Test
    void runtimeSourcesDoNotContainForbiddenTradingTokens() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> forbiddenTokens = List.of(
                "LedgerPostingService",
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

        try (var files = Files.walk(sourceRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
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
    }

    /**
     * 確認 posting boundary 仍沒有直接引用 append adapter，避免 P15 把正式 money movement 路徑提早接起來。
     *
     * 這個 case 必須存在，因為 trading runtime core 的第一步只能是 scope gate，不能直接跨層連到 JDBC adapter。
     */
    @Test
    void postingBoundaryStillDoesNotReferenceAppendAdapter() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Path commandBoundary = sourceRoot.resolve("com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java");
        Path defaultBoundary = sourceRoot.resolve("com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java");
        Path adapter = sourceRoot.resolve("com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java");

        String commandBoundarySource = Files.readString(commandBoundary, StandardCharsets.UTF_8);
        String defaultBoundarySource = Files.readString(defaultBoundary, StandardCharsets.UTF_8);
        String adapterSource = Files.readString(adapter, StandardCharsets.UTF_8);

        assertFalse(commandBoundarySource.contains("LedgerAppendOnlyJdbcAdapter"));
        assertFalse(defaultBoundarySource.contains("LedgerAppendOnlyJdbcAdapter"));
        assertFalse(adapterSource.contains("LedgerPostingCommandBoundary"));
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
