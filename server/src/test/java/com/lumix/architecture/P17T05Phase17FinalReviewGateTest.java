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
 * 驗證 Phase 17 final review gate 在人工批准後會收斂為 completed，
 * 但仍不會把 futures sandbox foundation 誤寫成正式 futures trading。
 */
class P17T05Phase17FinalReviewGateTest {

    /**
     * 確認 Phase 17 的狀態文件在人工批准後統一使用 completed / approved 語意，
     * 且 final review 文案沒有把 sandbox core model 誤寫成 production-ready 或正式 futures trading。
     */
    @Test
    void phase17StatusDocumentsBecomeCompletedButRemainNotProductionReady() throws IOException {
        Path docsRoot = resolveDocsRoot();
        Path repoRoot = docsRoot.getParent().getParent().getParent();
        String phaseReadme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        String phaseIndex = Files.readString(repoRoot.resolve("docs/phases/README.md"), StandardCharsets.UTF_8);
        String masterPlan = Files.readString(repoRoot.resolve("docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md"), StandardCharsets.UTF_8);
        String progress = Files.readString(repoRoot.resolve("AI_PROGRESS.md"), StandardCharsets.UTF_8);
        String finalReview = Files.readString(docsRoot.resolve("phase-17-final-review.md"), StandardCharsets.UTF_8);

        assertTrue(phaseReadme.contains("completed"));
        assertTrue(phaseReadme.contains("T05 no-formal-trading review - completed"));
        assertTrue(phaseReadme.contains("sandbox core model"));
        assertTrue(phaseReadme.contains("no real money"));
        assertTrue(phaseReadme.contains("no order intake"));
        assertTrue(phaseReadme.contains("no matching"));
        assertTrue(phaseReadme.contains("no settlement"));
        assertTrue(phaseReadme.contains("no ledger mutation"));
        assertTrue(phaseReadme.contains("no balance reservation"));
        assertTrue(phaseReadme.contains("no liquidation"));
        assertTrue(phaseReadme.contains("no funding"));
        assertTrue(phaseReadme.contains("phase-17-final-review.md"));
        assertTrue(phaseReadme.contains("Phase 17 人工審核完成"));
        assertFalse(phaseReadme.contains("Phase 17 production-ready"));
        assertFalse(phaseReadme.contains("Phase 17 futures trading ready"));
        assertFalse(phaseReadme.contains("order accepted"));
        assertFalse(phaseReadme.contains("margin reserved"));
        assertFalse(phaseReadme.contains("position opened"));

        assertTrue(phaseIndex.contains("PHASE_17_ORDER_INTAKE/         completed"));
        assertFalse(phaseIndex.contains("PHASE_17_ORDER_INTAKE/         implementation completed / pending human review"));

        assertTrue(masterPlan.contains("Phase 17: COMPLETED"));
        assertTrue(masterPlan.contains("Phase 17 human review: APPROVED"));
        assertTrue(masterPlan.contains("Phase 17 人工審核完成"));
        assertTrue(masterPlan.contains("Futures core sandbox model foundation implemented"));
        assertTrue(masterPlan.contains("NOT production-ready"));
        assertTrue(masterPlan.contains("NOT public futures trading ready"));
        assertTrue(masterPlan.contains("NOT real-money ready"));
        assertTrue(masterPlan.contains("NOT order-intake-ready"));
        assertTrue(masterPlan.contains("NOT matching-ready"));
        assertTrue(masterPlan.contains("NOT settlement-ready"));
        assertTrue(masterPlan.contains("NOT ledger-integrated"));
        assertTrue(masterPlan.contains("NOT balance-reservation-backed"));
        assertTrue(masterPlan.contains("NOT liquidation-ready"));
        assertTrue(masterPlan.contains("NOT funding-ready"));
        assertTrue(masterPlan.contains("NOT full margin-engine-ready"));
        assertFalse(masterPlan.contains("Phase 17: Futures Core Model - next implementation phase, planned, not started"));

        assertTrue(progress.contains("Phase 17: COMPLETED"));
        assertTrue(progress.contains("Phase 17 human review: APPROVED"));
        assertTrue(progress.contains("Phase 17 人工審核完成"));
        assertTrue(progress.contains("Futures core sandbox model foundation implemented"));
        assertTrue(progress.contains("NOT production-ready"));
        assertTrue(progress.contains("NOT public futures trading ready"));
        assertTrue(progress.contains("NOT real-money ready"));
        assertTrue(progress.contains("NOT order-intake-ready"));
        assertTrue(progress.contains("NOT matching-ready"));
        assertTrue(progress.contains("NOT settlement-ready"));
        assertTrue(progress.contains("NOT ledger-integrated"));
        assertTrue(progress.contains("NOT balance-reservation-backed"));
        assertTrue(progress.contains("NOT liquidation-ready"));
        assertTrue(progress.contains("NOT funding-ready"));
        assertTrue(progress.contains("NOT full margin-engine-ready"));
        assertTrue(progress.contains("Phase 18: IN PROGRESS — T01 COMPLETED"));
        assertTrue(progress.contains("Phase 18 Futures Trading Sandbox started at T01 futures order placement only"));
        assertTrue(progress.contains("Phase 19-36: planned, not started"));
        assertTrue(progress.contains("Next implementation phase: none before explicit T02 kickoff; Phase 19 not started"));
        assertFalse(progress.contains("Phase 17: IMPLEMENTATION_COMPLETED_PENDING_HUMAN_REVIEW"));

        assertTrue(finalReview.contains("Phase 17: COMPLETED"));
        assertTrue(finalReview.contains("Phase 17 human review: APPROVED"));
        assertTrue(finalReview.contains("Phase 17 人工審核完成"));
        assertTrue(finalReview.contains("Futures core sandbox model foundation implemented"));
        assertTrue(finalReview.contains("NOT production-ready"));
        assertTrue(finalReview.contains("NOT public futures trading ready"));
        assertTrue(finalReview.contains("NOT real-money ready"));
        assertTrue(finalReview.contains("NOT order-intake-ready"));
        assertTrue(finalReview.contains("NOT matching-ready"));
        assertTrue(finalReview.contains("NOT settlement-ready"));
        assertTrue(finalReview.contains("NOT ledger-integrated"));
        assertTrue(finalReview.contains("NOT balance-reservation-backed"));
        assertTrue(finalReview.contains("NOT liquidation-ready"));
        assertTrue(finalReview.contains("NOT funding-ready"));
        assertTrue(finalReview.contains("NOT full margin-engine-ready"));
        assertTrue(finalReview.contains("Phase 17 human review approved"));
        assertFalse(finalReview.contains("Phase 17: IMPLEMENTATION_COMPLETED_PENDING_HUMAN_REVIEW"));
        assertFalse(finalReview.contains("Phase 17 production-ready"));
        assertTrue(finalReview.contains("## 禁止誤寫"));
        assertTrue(finalReview.contains("order accepted"));
        assertTrue(finalReview.contains("margin reserved"));
        assertTrue(finalReview.contains("balance frozen"));
        assertTrue(finalReview.contains("ledger posted"));
        assertTrue(finalReview.contains("position opened"));
        assertTrue(finalReview.contains("settlement completed"));
    }

    /**
     * 確認 futures source 只保存 sandbox model 與 pure gate，不會偷接成正式 runtime 或寫入 SQL。
     *
     * 這個 case 必須存在，因為 T05 的 no-formal-trading review 若沒有回歸測試，後續很容易在 futures package 偷渡 runtime 依賴。
     */
    @Test
    void futuresSourcesDoNotContainForbiddenRuntimeTokensOrWriteSql() throws IOException {
        Path sourceRoot = resolveFuturesSourceRoot();
        List<Path> sourceFiles;
        try (var files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }

        List<String> forbiddenTokens = List.of(
                "OrderIntakeService",
                "OrderPlacementService",
                "MatchingService",
                "MatchingEngine",
                "SettlementService",
                "SettlementEngine",
                "TradeExecutionService",
                "PositionService",
                "PositionCreationService",
                "ReservationService",
                "ReservationRuntimeService",
                "BalanceLookupService",
                "BalanceReservationService",
                "LedgerPostingService",
                "WalletService",
                "LiquidationService",
                "LiquidationEngine",
                "FundingService",
                "FundingRuntime",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "Connection",
                "PreparedStatement"
        );
        List<String> forbiddenSqlSnippets = List.of(
                "INSERT INTO orders",
                "UPDATE orders",
                "DELETE FROM orders",
                "SELECT * FROM orders",
                "INSERT INTO trades",
                "UPDATE trades",
                "DELETE FROM trades",
                "SELECT * FROM trades",
                "INSERT INTO positions",
                "UPDATE positions",
                "DELETE FROM positions",
                "SELECT * FROM positions",
                "INSERT INTO reservations",
                "UPDATE reservations",
                "DELETE FROM reservations",
                "SELECT * FROM reservations",
                "INSERT INTO balance_projections",
                "UPDATE balance_projections",
                "DELETE FROM balance_projections",
                "SELECT * FROM balance_projections",
                "INSERT INTO ledger_journals",
                "UPDATE ledger_journals",
                "DELETE FROM ledger_journals",
                "SELECT * FROM ledger_journals",
                "INSERT INTO ledger_entries",
                "UPDATE ledger_entries",
                "DELETE FROM ledger_entries",
                "SELECT * FROM ledger_entries",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "SELECT * FROM outbox_events"
        );

        for (Path javaFile : sourceFiles) {
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

    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_17_ORDER_INTAKE");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("../docs/phases/PHASE_17_ORDER_INTAKE");
    }

    private static Path resolveFuturesSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java/com/lumix/trading/core/futures");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java/com/lumix/trading/core/futures");
    }
}
