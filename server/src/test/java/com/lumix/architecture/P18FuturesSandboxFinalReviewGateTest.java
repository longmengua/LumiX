package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 驗證 P18 在人類批准後只收斂為 sandbox foundation，避免文件回歸成正式合約交易宣稱。
 */
class P18FuturesSandboxFinalReviewGateTest {

    /**
     * 確認 Phase 18 的 completed / approved 狀態與未完成 runtime 會一起保留，讓後續 phase 不會誤讀審核結論。
     */
    @Test
    void phase18FinalReviewRemainsSandboxOnlyAfterHumanApproval() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        String finalReview = Files.readString(docsRoot.resolve("phase-18-final-review.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("completed — Futures Trading Sandbox foundation; HUMAN_REVIEW_REQUIRED approved"));
        assertTrue(readme.contains("Phase 18 人工審核完成"));
        assertTrue(readme.contains("phase-18-final-review.md"));
        assertTrue(finalReview.contains("Phase 18: COMPLETED_FOR_FUTURES_TRADING_SANDBOX_FOUNDATION"));
        assertTrue(finalReview.contains("Phase 18 human review: APPROVED"));
        assertTrue(finalReview.contains("NOT production-ready"));
        assertTrue(finalReview.contains("NOT real-money ready"));
        assertTrue(finalReview.contains("NOT matching-execution-ready"));
        assertTrue(finalReview.contains("NOT settlement-ready"));
        assertTrue(finalReview.contains("NOT liquidation-ready"));
        assertTrue(finalReview.contains("Phase 19-T01 liquidation simulation"));
        assertFalse(finalReview.contains("Phase 18 production-ready"));
        assertFalse(finalReview.contains("Phase 18 futures trading ready"));
        assertFalse(finalReview.contains("Phase 18 real-money futures ready"));
    }

    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_18_MATCHING_CONTRACT");
        return Files.isDirectory(repoRelative) ? repoRelative : Path.of("../docs/phases/PHASE_18_MATCHING_CONTRACT");
    }
}
