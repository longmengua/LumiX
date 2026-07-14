package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 15 final review gate 只整理已完成內容與未完成 runtime，不會誤寫成 production-ready。
 */
class P15T09Phase15FinalReviewGateTest {

    /**
     * 確認 Phase 15 的狀態文件統一使用 COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION，且 final review 文案沒有誤把 runtime 寫成正式完成。
     *
     * 這個 case 必須存在，因為 final review 一旦寫錯，就會把 foundation gates 誤標成正式交易完成。
     */
    @Test
    void phase15StatusDocumentsRemainNotProductionReady() throws IOException {
        Path docsRoot = resolveDocsRoot();
        Path repoRoot = docsRoot.getParent().getParent().getParent();
        String phaseReadme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        String phaseIndex = Files.readString(repoRoot.resolve("docs/phases/README.md"), StandardCharsets.UTF_8);
        String masterPlan = Files.readString(repoRoot.resolve("docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md"), StandardCharsets.UTF_8);
        String progress = Files.readString(repoRoot.resolve("AI_PROGRESS.md"), StandardCharsets.UTF_8);
        String finalReview = Files.readString(docsRoot.resolve("phase-15-final-review.md"), StandardCharsets.UTF_8);

        assertTrue(phaseReadme.contains("COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION"));
        assertTrue(phaseReadme.contains("Phase 15 backend foundation gates completed"));
        assertTrue(phaseReadme.contains("Phase 15 trading runtime core foundation completed"));
        assertTrue(phaseReadme.contains("NOT production-ready"));
        assertTrue(phaseReadme.contains("NOT full trading runtime"));
        assertTrue(phaseReadme.contains("NOT order/matching/settlement ready"));
        assertTrue(phaseReadme.contains("NOT reservation runtime ready"));
        assertTrue(phaseReadme.contains("NOT settlement runtime ready"));
        assertTrue(phaseReadme.contains("NOT futures/liquidation/withdrawal ready"));
        assertTrue(phaseReadme.contains("NOT exchange ready"));
        assertTrue(phaseReadme.contains("NOT public user trading ready"));
        assertTrue(phaseReadme.contains("任何把 Phase 15 誤寫成 production-ready 的行為都屬於 HUMAN_REVIEW_REQUIRED。"));
        assertFalse(phaseReadme.contains("in progress"));
        assertFalse(phaseReadme.contains("Phase 15 production-ready"));
        assertFalse(phaseReadme.contains("trading system completed"));

        assertTrue(phaseIndex.contains("COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION"));
        assertTrue(phaseIndex.contains("NOT exchange ready"));
        assertTrue(phaseIndex.contains("NOT public user trading ready"));
        assertFalse(phaseIndex.contains("in progress (Trading Runtime Core foundation/review gates only)"));

        assertTrue(masterPlan.contains("Phase 15: COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION"));
        assertTrue(masterPlan.contains("NOT reservation runtime ready"));
        assertTrue(masterPlan.contains("NOT settlement runtime ready"));
        assertTrue(masterPlan.contains("NOT futures/liquidation/withdrawal ready"));
        assertTrue(masterPlan.contains("NOT exchange ready"));
        assertTrue(masterPlan.contains("NOT public user trading ready"));
        assertTrue(masterPlan.contains("P15 Trading Runtime Core"));
        assertTrue(masterPlan.contains("P18 Futures Trading Sandbox"));
        assertFalse(masterPlan.contains("Phase 15: Trading Runtime Core - in progress, foundation/review gates only"));

        assertTrue(progress.contains("Phase 15: COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION"));
        assertTrue(progress.contains("NOT reservation runtime ready"));
        assertTrue(progress.contains("NOT settlement runtime ready"));
        assertTrue(progress.contains("NOT futures/liquidation/withdrawal ready"));
        assertTrue(progress.contains("NOT exchange ready"));
        assertTrue(progress.contains("NOT public user trading ready"));
        assertTrue(progress.contains("Next implementation phase: Phase 17 - Futures Core Model"));
        assertFalse(progress.contains("Phase 15: in progress as trading runtime core foundation/review gates, runtime implementation incomplete"));

        assertTrue(finalReview.contains("Phase 15: COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION"));
        assertTrue(finalReview.contains("Phase 15 backend foundation gates completed"));
        assertTrue(finalReview.contains("Phase 15 trading runtime core foundation completed"));
        assertTrue(finalReview.contains("NOT production-ready"));
        assertTrue(finalReview.contains("NOT full trading runtime"));
        assertTrue(finalReview.contains("NOT order/matching/settlement ready"));
        assertTrue(finalReview.contains("NOT reservation runtime ready"));
        assertTrue(finalReview.contains("NOT settlement runtime ready"));
        assertTrue(finalReview.contains("NOT futures/liquidation/withdrawal ready"));
        assertTrue(finalReview.contains("NOT exchange ready"));
        assertTrue(finalReview.contains("NOT public user trading ready"));
        assertFalse(finalReview.contains("Phase 15: production-ready"));
        assertFalse(finalReview.contains("Phase 15: trading system completed"));
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
