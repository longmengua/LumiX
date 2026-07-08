package com.lumix.ledger.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

/**
 * 驗證 ledger runtime boundary 目前只做 prerequisite gate，不會偷偷進入 posting runtime。
 */
class LedgerRuntimeBoundaryTest {

    private final LedgerRuntimeBoundary boundary = new DefaultLedgerRuntimeBoundary();

    /**
     * 確認在沒有任何 prerequisite 時，boundary 會誠實列出缺漏項目。
     *
     * 這個測試保護的是 scope gate：ledger runtime 不能在 identity / account / asset / market
     * 與 Phase 12 schema foundation 都沒有到位之前開始跑。
     */
    @Test
    void missingPrerequisitesAreReported() {
        LedgerRuntimePrerequisiteReport report = boundary.verifyPrerequisites(
                EnumSet.noneOf(LedgerRuntimePrerequisite.class)
        );

        assertFalse(report.isReady());
        assertEquals(EnumSet.allOf(LedgerRuntimePrerequisite.class), report.missing());
        assertEquals(EnumSet.noneOf(LedgerRuntimePrerequisite.class), report.available());
    }

    /**
     * 確認所有 prerequisite 都到位時，boundary 只會回報 ready，不會做任何資金異動。
     *
     * 這個 case 很重要，因為它驗證的是 boundary readiness，而不是 posting 行為本身。
     */
    @Test
    void allPrerequisitesReadyMarksBoundaryReady() {
        LedgerRuntimePrerequisiteReport report = boundary.verifyPrerequisites(
                EnumSet.allOf(LedgerRuntimePrerequisite.class)
        );

        assertTrue(report.isReady());
        assertTrue(report.missing().isEmpty());
        assertEquals(EnumSet.allOf(LedgerRuntimePrerequisite.class), report.available());
    }
}
