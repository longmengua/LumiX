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
 * 驗證 P19-T01 只提供 liquidation simulation，禁止接入強平或帳務 runtime。
 */
class P19T01LiquidationSimulationScopeGateTest {

    @Test
    void liquidationSourcesContainNoExecutionOrMoneyMutationRuntime() throws IOException {
        Path sourceRoot = resolveSourceRoot().resolve("com/lumix/trading/core/futures/sandbox/liquidation");
        List<String> forbiddenTokens = List.of("FuturesSandboxPositionUpdateGate", "FuturesSandboxVerifiedFill", "LedgerPosting",
                "SettlementRuntime", "ReservationRuntime", "@Repository", "@Transactional", "JdbcTemplate", "EntityManager", "PreparedStatement");
        try (var files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                for (String token : forbiddenTokens) {
                    assertFalse(source.contains(token), () -> javaFile + " contains forbidden P19-T01 token: " + token);
                }
            }
        }
    }

    @Test
    void phase19ReadmeRecordsSimulationOnlyBoundary() throws IOException {
        String readme = Files.readString(resolveDocsRoot().resolve("README.md"), StandardCharsets.UTF_8);
        assertTrue(readme.contains("T01 liquidation simulation - completed"));
        assertTrue(readme.contains("不產生強平單、不關閉 position、不更新 balance、ledger 或 settlement"));
    }

    private static Path resolveSourceRoot() {
        Path root = Path.of("server/src/main/java");
        return Files.isDirectory(root) ? root : Path.of("src/main/java");
    }

    private static Path resolveDocsRoot() {
        Path root = Path.of("docs/phases/PHASE_19_SETTLEMENT_ENGINE");
        return Files.isDirectory(root) ? root : Path.of("../docs/phases/PHASE_19_SETTLEMENT_ENGINE");
    }
}
