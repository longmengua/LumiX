package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 14 final review 文件已完成，且沒有把 foundation 誤寫成 production-ready。
 */
class P14T10Phase14FinalReviewGateTest {

    /**
     * 確認 Phase 14 README 與 final review 文件都有明確標示 completed 與非 production-ready。
     *
     * 這個 case 必須存在，因為 final review gate 的核心就是把現況說清楚，不能只留下半成品語意。
     */
    @Test
    void finalReviewDocumentsStatePhase14Clearly() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        String finalReview = Files.readString(docsRoot.resolve("final-review.md"), StandardCharsets.UTF_8);
        String noGo = Files.readString(docsRoot.resolve("runtime-integration-no-go.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("completed"));
        assertTrue(readme.contains("P14-T10 phase 14 final review gate"));
        assertTrue(finalReview.contains("Immutable Ledger Engine foundation completed"));
        assertTrue(finalReview.contains("append-only adapter verified on PostgreSQL"));
        assertTrue(finalReview.contains("not production-ready"));
        assertTrue(finalReview.contains("not full ledger posting runtime"));
        assertTrue(finalReview.contains("Remaining Risks"));
        assertTrue(finalReview.contains("HUMAN_REVIEW_REQUIRED"));
        assertTrue(noGo.contains("LedgerPostingCommandBoundary 沒有接 LedgerAppendOnlyJdbcAdapter"));
    }

    /**
     * 確認 Phase 14 仍有清楚列出 T01 到 T10 的任務序列。
     *
     * 這個 case 必須存在，因為 final review 需要讓後續維護者知道這一階段的範圍邊界已經收斂。
     */
    @Test
    void phase14READMEListsAllTasksThroughT10() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("P14-T01 scope gate and runtime prerequisites"));
        assertTrue(readme.contains("P14-T02 ledger journal draft and invariant contract"));
        assertTrue(readme.contains("P14-T03 ledger persistence port and append-only mapping contract"));
        assertTrue(readme.contains("P14-T04 ledger posting application command boundary"));
        assertTrue(readme.contains("P14-T05 ledger append transaction boundary design"));
        assertTrue(readme.contains("P14-T06 ledger append persistence adapter implementation gate"));
        assertTrue(readme.contains("P14-T07 PostgreSQL verification for ledger append adapter"));
        assertTrue(readme.contains("P14-T08 ledger idempotency and request identity design gate"));
        assertTrue(readme.contains("P14-T09 ledger runtime integration no-go gate"));
        assertTrue(readme.contains("P14-T10 phase 14 final review gate"));
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_14_LEDGER_ENGINE");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("../docs/phases/PHASE_14_LEDGER_ENGINE");
    }
}
