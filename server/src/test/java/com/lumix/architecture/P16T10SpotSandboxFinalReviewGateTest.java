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
 * 驗證 Phase 16 final review gate 只會收斂 spot sandbox foundation 狀態，不會誤寫成正式交易完成。
 */
class P16T10SpotSandboxFinalReviewGateTest {

    /**
     * 確認 Phase 16 文件已統一為 COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION，且 final review 文案沒有誤把 sandbox foundation 寫成 production-ready。
     *
     * 這個 case 必須存在，因為 final review 文字一旦錯誤，就會把 sandbox flow foundation 誤標成正式交易上線。
     */
    @Test
    void phase16StatusDocumentsRemainNotProductionReady() throws IOException {
        Path docsRoot = resolveDocsRoot();
        Path repoRoot = docsRoot.getParent().getParent().getParent();
        String phaseReadme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        String phaseIndex = Files.readString(repoRoot.resolve("docs/phases/README.md"), StandardCharsets.UTF_8);
        String masterPlan = Files.readString(repoRoot.resolve("docs/OPERATING_EXCHANGE_MASTER_PLAN.md"), StandardCharsets.UTF_8);
        String progress = Files.readString(repoRoot.resolve("AI_PROGRESS.md"), StandardCharsets.UTF_8);
        String finalReview = Files.readString(docsRoot.resolve("phase-16-final-review.md"), StandardCharsets.UTF_8);

        assertTrue(phaseReadme.contains("Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION"));
        assertTrue(phaseReadme.contains("Spot sandbox flow foundation completed"));
        assertTrue(phaseReadme.contains("NOT production-ready"));
        assertTrue(phaseReadme.contains("NOT public user trading ready"));
        assertTrue(phaseReadme.contains("NOT real-money ready"));
        assertTrue(phaseReadme.contains("NOT ledger-posting-integrated"));
        assertTrue(phaseReadme.contains("NOT balance-updating"));
        assertTrue(phaseReadme.contains("NOT reservation-backed"));
        assertTrue(phaseReadme.contains("NOT settlement-finalized"));
        assertTrue(phaseReadme.contains("NOT withdrawal-ready"));
        assertTrue(phaseReadme.contains("NOT futures/margin/liquidation ready"));
        assertTrue(phaseReadme.contains("P16-T10 completed as final review gate only"));
        assertFalse(phaseReadme.contains("in progress"));
        assertFalse(phaseReadme.contains("Phase 16 production-ready"));
        assertFalse(phaseReadme.contains("Phase 16 exchange ready"));
        assertFalse(phaseReadme.contains("Phase 16 public trading ready"));
        assertFalse(phaseReadme.contains("Phase 16 real-money ready"));
        assertFalse(phaseReadme.contains("Phase 16 ledger posted"));
        assertFalse(phaseReadme.contains("Phase 16 balance updated"));
        assertFalse(phaseReadme.contains("Phase 16 reservation committed"));
        assertFalse(phaseReadme.contains("Phase 16 settlement finalized"));
        assertFalse(phaseReadme.contains("Phase 16 full trading runtime completed"));

        assertTrue(phaseIndex.contains("COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION"));
        assertTrue(phaseIndex.contains("NOT production-ready"));
        assertTrue(phaseIndex.contains("NOT public user trading ready"));
        assertFalse(phaseIndex.contains("in progress (Spot Trading Sandbox scope gate, boundaries, and sandbox runtime foundation only)"));

        assertTrue(masterPlan.contains("Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION"));
        assertTrue(masterPlan.contains("NOT production-ready"));
        assertTrue(masterPlan.contains("NOT public user trading ready"));
        assertTrue(masterPlan.contains("NOT real-money ready"));
        assertTrue(masterPlan.contains("NOT ledger-posting-integrated"));
        assertTrue(masterPlan.contains("NOT balance-updating"));
        assertTrue(masterPlan.contains("NOT reservation-backed"));
        assertTrue(masterPlan.contains("NOT settlement-finalized"));
        assertTrue(masterPlan.contains("NOT withdrawal-ready"));
        assertTrue(masterPlan.contains("NOT futures/margin/liquidation ready"));
        assertFalse(masterPlan.contains("Phase 16: Spot Trading Sandbox - in progress"));

        assertTrue(progress.contains("Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION"));
        assertTrue(progress.contains("NOT production-ready"));
        assertTrue(progress.contains("NOT public user trading ready"));
        assertTrue(progress.contains("NOT real-money ready"));
        assertTrue(progress.contains("NOT ledger-posting-integrated"));
        assertTrue(progress.contains("NOT balance-updating"));
        assertTrue(progress.contains("NOT reservation-backed"));
        assertTrue(progress.contains("NOT settlement-finalized"));
        assertTrue(progress.contains("NOT withdrawal-ready"));
        assertTrue(progress.contains("NOT futures/margin/liquidation ready"));
        assertTrue(progress.contains("Next implementation phase: Phase 17 - Futures Core Model"));
        assertFalse(progress.contains("Phase 16: in progress as spot trading sandbox scope gate/boundary/runtime foundation, runtime implementation incomplete"));

        assertTrue(finalReview.contains("Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION"));
        assertTrue(finalReview.contains("Spot sandbox flow foundation completed"));
        assertTrue(finalReview.contains("NOT production-ready"));
        assertTrue(finalReview.contains("NOT public user trading ready"));
        assertTrue(finalReview.contains("NOT real-money ready"));
        assertTrue(finalReview.contains("NOT ledger-posting-integrated"));
        assertTrue(finalReview.contains("NOT balance-updating"));
        assertTrue(finalReview.contains("NOT reservation-backed"));
        assertTrue(finalReview.contains("NOT settlement-finalized"));
        assertTrue(finalReview.contains("NOT withdrawal-ready"));
        assertTrue(finalReview.contains("NOT futures/margin/liquidation ready"));
        assertFalse(finalReview.contains("Phase 16 in progress"));
        assertFalse(finalReview.contains("Phase 16 production-ready"));
        assertFalse(finalReview.contains("Phase 16 exchange ready"));
        assertFalse(finalReview.contains("Phase 16 public trading ready"));
        assertFalse(finalReview.contains("Phase 16 real-money ready"));
        assertFalse(finalReview.contains("Phase 16 ledger posted"));
        assertFalse(finalReview.contains("Phase 16 balance updated"));
        assertFalse(finalReview.contains("Phase 16 reservation committed"));
        assertFalse(finalReview.contains("Phase 16 settlement finalized"));
        assertFalse(finalReview.contains("Phase 16 full trading runtime completed"));
    }

    /**
     * 確認 final review boundary source 只有狀態/審查語意，不會混入正式 runtime token 或 SQL。
     *
     * 這個 case 必須存在，因為 final review boundary 若出現 runtime 名稱，就可能被誤解成已接上正式交易路徑。
     */
    @Test
    void finalReviewBoundarySourceDoesNotContainForbiddenRuntimeTokensOrWriteSql() throws IOException {
        Path sourceFile = resolveSourceRoot().resolve("SpotSandboxFinalReviewBoundary.java");
        String source = Files.readString(sourceFile, StandardCharsets.UTF_8);

        List<String> forbiddenTokens = List.of(
                "SettlementService",
                "SettlementEngine",
                "TradeSettlementService",
                "MatchingService",
                "MatchingEngine",
                "TradeService",
                "ReservationRuntimeService",
                "ReservationService",
                "OrderPlacementService",
                "SpotTradingService",
                "LedgerPostingService",
                "BalanceMutationService",
                "BalanceProjectionService",
                "ExchangeService",
                "ProductionTradingService",
                "LedgerPostingRuntimeGate",
                "LedgerAppendOnlyJdbcAdapter",
                "BalanceProjectionRebuildGate",
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
                "INSERT INTO idempotency_keys",
                "UPDATE idempotency_keys",
                "DELETE FROM idempotency_keys",
                "SELECT * FROM idempotency_keys",
                "INSERT INTO outbox_events",
                "UPDATE outbox_events",
                "DELETE FROM outbox_events",
                "SELECT * FROM outbox_events",
                "INSERT INTO audit_logs",
                "UPDATE audit_logs",
                "DELETE FROM audit_logs",
                "SELECT * FROM audit_logs"
        );

        for (String forbiddenToken : forbiddenTokens) {
            assertFalse(source.contains(forbiddenToken),
                    () -> sourceFile + " contains forbidden token: " + forbiddenToken);
        }
        for (String forbiddenSqlSnippet : forbiddenSqlSnippets) {
            assertFalse(source.contains(forbiddenSqlSnippet),
                    () -> sourceFile + " contains forbidden SQL: " + forbiddenSqlSnippet);
        }
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_16_SPOT_TRADING_SANDBOX");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("../docs/phases/PHASE_16_SPOT_TRADING_SANDBOX");
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java/com/lumix/trading/core/spot/review");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java/com/lumix/trading/core/spot/review");
    }
}
