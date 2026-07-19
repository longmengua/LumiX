package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證 P20 只收斂 sandbox integration gate，避免文件或 source 被誤解成正式合約交易 runtime。
 *
 * 此測試檢查禁止宣稱與架構邊界；即使人類批准 P20 sandbox foundation，也不能把它升格為正式合約交易 runtime。
 */
class P20T05ProductionNoClaimGateTest {

    @Test
    void phase20DocumentationRecordsSandboxScopeAndProductionNoClaimBoundary() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        String noClaimReview = Files.readString(docsRoot.resolve("phase-20-no-claim-review.md"), StandardCharsets.UTF_8);
        String finalReview = Files.readString(docsRoot.resolve("phase-20-final-review.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("completed — Contract Trading Integration Gate foundation; HUMAN_REVIEW_REQUIRED approved"));
        assertTrue(readme.contains("T01-T05 已完成"));
        assertTrue(readme.contains("最終審核已由人類於 2026-07-20 批准"));
        assertTrue(noClaimReview.contains("T01 contract eligibility 與 liquidation simulation 的 pure flow integration"));
        assertTrue(noClaimReview.contains("T04 read-only admin / audit review snapshot"));
        assertTrue(noClaimReview.contains("NOT production-ready"));
        assertTrue(noClaimReview.contains("NOT formal contract trading launched"));
        assertTrue(noClaimReview.contains("NOT public contract trading ready"));
        assertTrue(noClaimReview.contains("NOT real-money contract trading ready"));
        assertTrue(noClaimReview.contains("NOT settlement completed"));
        assertTrue(noClaimReview.contains("final review 已由人類於 2026-07-20 批准"));
        assertTrue(finalReview.contains("Phase 20: COMPLETED_FOR_CONTRACT_TRADING_INTEGRATION_GATE_FOUNDATION"));
        assertTrue(finalReview.contains("Phase 20 human review: APPROVED"));
        assertTrue(finalReview.contains("Phase 20 人工審核完成"));
        assertTrue(finalReview.contains("Next task: define and review the next phase task before implementation"));
        assertFalse(finalReview.contains("Phase 20 human review: NOT APPROVED"));
    }

    @Test
    void phase20IntegrationSourcesDoNotContainProductionRuntimeOrWriteSql() throws IOException {
        List<String> forbiddenTokens = List.of(
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "DataSource",
                "MatchingEngine",
                "TradeExecution",
                "LedgerPosting",
                "BalanceProjection",
                "SettlementEngine",
                "AdminAuthorization",
                "INSERT INTO",
                "UPDATE ",
                "DELETE FROM",
                "SELECT * FROM"
        );

        try (Stream<Path> sourceFiles = Files.list(resolveIntegrationSourceRoot())) {
            for (Path sourceFile : sourceFiles.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
                for (String forbiddenToken : forbiddenTokens) {
                    assertFalse(
                            source.contains(forbiddenToken),
                            () -> sourceFile + " contains forbidden production runtime token: " + forbiddenToken
                    );
                }
            }
        }
    }

    /** 讓測試同時支援從 repo root 與 server 目錄執行。 */
    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_20_FEE_ENGINE");
        return Files.isDirectory(repoRelative) ? repoRelative : Path.of("../docs/phases/PHASE_20_FEE_ENGINE");
    }

    /** 讓 source boundary test 同時支援從 repo root 與 server 目錄執行。 */
    private static Path resolveIntegrationSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java/com/lumix/trading/core/futures/sandbox/integration");
        return Files.isDirectory(repoRelative)
                ? repoRelative
                : Path.of("src/main/java/com/lumix/trading/core/futures/sandbox/integration");
    }
}
