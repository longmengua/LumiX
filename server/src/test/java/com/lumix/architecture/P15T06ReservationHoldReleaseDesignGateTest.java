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
 * 驗證 Phase 15-T06 只建立 reservation hold/release design gate，不會偷接成正式 reservation runtime。
 */
class P15T06ReservationHoldReleaseDesignGateTest {

    /**
     * 確認 reservation design sources 沒有正式 runtime token、repository/transaction annotation 或 reservation/balance/ledger mutation SQL。
     *
     * 這個 case 必須存在，因為 reservation 會直接影響 available / locked balance，任何 runtime token 都會讓設計 gate 失焦。
     */
    @Test
    void reservationDesignSourcesDoNotContainForbiddenRuntimeTokensOrMutationSql() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        List<String> sourceFiles = List.of(
                "com/lumix/trading/core/package-info.java",
                "com/lumix/trading/core/TradingRuntimeCoreScope.java",
                "com/lumix/trading/core/TradingRuntimeCoreSafetyContract.java",
                "com/lumix/trading/core/TradingRuntimeCoreSafetyPolicy.java",
                "com/lumix/trading/core/reservation/package-info.java",
                "com/lumix/trading/core/reservation/ReservationLifecycleDecision.java",
                "com/lumix/trading/core/reservation/ReservationOperationType.java",
                "com/lumix/trading/core/reservation/ReservationHoldReleaseDesign.java",
                "com/lumix/trading/core/reservation/ReservationHoldReleaseDesignPolicy.java"
        );

        List<String> forbiddenTokens = List.of(
                "ReservationRuntimeService",
                "ReservationService",
                "BalanceMutationService",
                "BalanceProjectionService",
                "SettlementService",
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
                "DELETE FROM ledger_entries"
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
     * 確認 Phase 15 README 與 task 索引已列出 reservation design gate。
     *
     * 這個 case 必須存在，因為 design gate 的文件索引就是後續施工與審核的入口。
     */
    @Test
    void phase15READMEAndTaskIndexIncludeT06() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T06 reservation hold/release design gate"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("reservation-hold-release-design.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T06.md")));
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
