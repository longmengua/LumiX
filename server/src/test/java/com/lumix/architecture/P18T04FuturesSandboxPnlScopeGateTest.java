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
 * 驗證 Phase 18-T04 只建立 PnL 純計算與 close preview，不偷接價格來源、部位異動或資金 runtime。
 */
class P18T04FuturesSandboxPnlScopeGateTest {

    /**
     * 確認 PnL source 沒有 mark-price provider、position update、交易或 persistence 依賴。
     */
    @Test
    void pnlSourcesContainNoPriceSourcePositionMutationOrMoneyRuntime() throws IOException {
        Path sourceRoot = resolveSourceRoot().resolve("com/lumix/trading/core/futures/pnl");
        List<String> forbiddenTokens = List.of(
                "MarkPriceService",
                "DefaultMarkPriceService",
                "FuturesSandboxMatchingGate",
                "FuturesSandboxPositionUpdateGate",
                "createTradeFill",
                "ReservationRuntime",
                "LedgerPosting",
                "SettlementRuntime",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "Connection",
                "PreparedStatement"
        );

        try (var files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                for (String forbiddenToken : forbiddenTokens) {
                    assertFalse(source.contains(forbiddenToken),
                            () -> javaFile + " contains forbidden T04 token: " + forbiddenToken);
                }
            }
        }
    }

    /**
     * 確認 phase 文件清楚將 realized PnL 限制為不修改 position 的 preview。
     */
    @Test
    void phase18ReadmeRecordsT04PurePnlBoundary() throws IOException {
        String readme = Files.readString(resolveDocsRoot().resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T04 realized / unrealized PnL - completed"));
        assertTrue(readme.contains("realized PnL 只提供不改變 position 的 close preview"));
    }

    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java");
        return Files.isDirectory(repoRelative) ? repoRelative : Path.of("src/main/java");
    }

    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_18_MATCHING_CONTRACT");
        return Files.isDirectory(repoRelative) ? repoRelative : Path.of("../docs/phases/PHASE_18_MATCHING_CONTRACT");
    }
}
