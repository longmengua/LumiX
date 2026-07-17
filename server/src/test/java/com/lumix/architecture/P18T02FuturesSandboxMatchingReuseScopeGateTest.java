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
 * 驗證 Phase 18-T02 只重用 pure 限價單候選配對規則，不會偷接成 futures matching execution。
 */
class P18T02FuturesSandboxMatchingReuseScopeGateTest {

    /**
     * 確認 futures 與共用候選配對 source 沒有 persistence、交易、倉位或資金 runtime 依賴。
     *
     * 這個 case 必須存在，因為 T02 的 MATCH_ELIGIBLE 只能作為下一階段輸入，不能被誤認為已 fill 或已開倉。
     */
    @Test
    void matchingReuseSourcesContainNoExecutionOrMoneyRuntimeTokens() throws IOException {
        List<Path> sourceRoots = List.of(
                resolveSourceRoot().resolve("com/lumix/trading/core/futures/matching"),
                resolveSourceRoot().resolve("com/lumix/trading/core/sandbox/matching")
        );
        List<String> forbiddenTokens = List.of(
                "InMemorySpotSandboxOrderBook",
                "SpotSandboxTradeFill",
                "FILLED",
                "PARTIALLY_FILLED",
                "PositionUpdate",
                "PnlRuntime",
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

        for (Path sourceRoot : sourceRoots) {
            try (var files = Files.walk(sourceRoot)) {
                for (Path javaFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                    for (String forbiddenToken : forbiddenTokens) {
                        assertFalse(source.contains(forbiddenToken),
                                () -> javaFile + " contains forbidden T02 runtime token: " + forbiddenToken);
                    }
                }
            }
        }
    }

    /**
     * 確認 phase 文件清楚區分 candidate evaluation 與後續 matching execution。
     */
    @Test
    void phase18ReadmeRecordsT02AsCandidateReuseOnly() throws IOException {
        String readme = Files.readString(resolveDocsRoot().resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T02 matching reuse - completed"));
        assertTrue(readme.contains("不建立 order book、不產生 trade / fill、不更新 position"));
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
