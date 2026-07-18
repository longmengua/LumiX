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
 * 驗證 Phase 18-T03 只處理 verified-fill 到 immutable opening snapshot 的 pure sandbox boundary。
 */
class P18T03FuturesSandboxPositionUpdateScopeGateTest {

    /**
     * 確認 T03 不依賴 T02 candidate、不產生 fill，也沒有 persistence、資金或 PnL runtime。
     *
     * 這個 case 必須存在，因為 position opening 若把 candidate 視為 fill，會直接造成沒有成交依據的幽靈部位。
     */
    @Test
    void positionUpdateSourcesContainNoCandidateProducerOrRuntimeDependencies() throws IOException {
        Path sourceRoot = resolveSourceRoot().resolve("com/lumix/trading/core/futures/position/update");
        List<String> forbiddenTokens = List.of(
                "FuturesSandboxMatchCandidate",
                "FuturesSandboxMatchingGate",
                "InMemorySpotSandboxOrderBook",
                "createTradeFill",
                "PositionUpdateService",
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

        try (var files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                for (String forbiddenToken : forbiddenTokens) {
                    assertFalse(source.contains(forbiddenToken),
                            () -> javaFile + " contains forbidden T03 token: " + forbiddenToken);
                }
            }
        }
    }

    /**
     * 確認 phase 文件鎖定 one-way、open-only 與 verified-fill 輸入條件。
     */
    @Test
    void phase18ReadmeRecordsT03SandboxBoundary() throws IOException {
        String readme = Files.readString(resolveDocsRoot().resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T03 position update - completed"));
        assertTrue(readme.contains("one-way、open-only"));
        assertTrue(readme.contains("不得從 T02 candidate 直接更新 position"));
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
