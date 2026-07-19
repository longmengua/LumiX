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
 * 驗證 Phase 18-T06 只建立單一 market 的 sandbox inspection eligibility，不偷接交易或資金 runtime。
 */
class P18T06RestrictedFuturesSandboxContractScopeGateTest {

    /**
     * 確認 contract source 不依賴 matching、verified fill、position/PnL/funding 計算或 persistence transaction。
     */
    @Test
    void contractSourcesContainNoTradingOrMoneyMutationRuntime() throws IOException {
        Path sourceRoot = resolveSourceRoot().resolve("com/lumix/trading/core/futures/sandbox/contract");
        List<String> forbiddenTokens = List.of(
                "import com.lumix.trading.core.futures.matching.",
                "import com.lumix.trading.core.futures.position.update.",
                "import com.lumix.trading.core.futures.pnl.",
                "import com.lumix.trading.core.futures.sandbox.funding.",
                "import com.lumix.trading.core.reservation.",
                "import com.lumix.ledger.",
                "FuturesSandboxVerifiedFill",
                "FuturesSandboxPositionUpdateGate",
                "FuturesSandboxPnlCalculator",
                "FuturesSandboxFundingPreviewCalculator",
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
                            () -> javaFile + " contains forbidden T06 token: " + forbiddenToken);
                }
            }
        }
    }

    /**
     * 確認 phase 文件明確保留 T06 的 inspection 限制，避免被誤解為正式合約交易能力。
     */
    @Test
    void phase18ReadmeRecordsT06RestrictedInspectionBoundary() throws IOException {
        String readme = Files.readString(resolveDocsRoot().resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T06 restricted contract sandbox gate - completed"));
        assertTrue(readme.contains("inspection eligibility"));
        assertTrue(readme.contains("不產生 fill、trade、position、PnL/funding 套用"));
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
