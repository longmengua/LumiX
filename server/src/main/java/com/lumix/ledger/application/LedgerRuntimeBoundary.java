package com.lumix.ledger.application;

import java.util.Set;

/**
 * ledger runtime 的 application boundary 入口。
 *
 * 這個 boundary 目前只做 prerequisite 檢查與 readiness 回報，
 * 不提供任何 posting、reserve、release 或 settlement runtime。
 */
public interface LedgerRuntimeBoundary {

    /**
     * 檢查 ledger runtime 是否已具備啟動條件。
     *
     * 只有在 identity、account、asset、market 與 Phase 12 schema foundation 都就位時，
     * 這個 boundary 才應回報 ready。
     */
    LedgerRuntimePrerequisiteReport verifyPrerequisites(Set<LedgerRuntimePrerequisite> availablePrerequisites);
}
