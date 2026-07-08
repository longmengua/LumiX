package com.lumix.ledger;

import com.lumix.ledger.application.LedgerRuntimePrerequisite;
import com.lumix.ledger.application.LedgerRuntimePrerequisiteReport;

import java.util.Set;

/**
 * ledger bounded context 的高風險契約入口。
 *
 * Phase 14-T01 只保留 runtime prerequisite inspection，不提供任何 posting、
 * reserve、release、commit、rollback 或 balance mutation 語意。
 */
public interface LedgerService {

    /**
     * 檢查 ledger runtime 是否已具備啟動條件。
     *
     * 這個 contract 只回報 prerequisite 狀態，不應觸發任何資金異動。
     */
    LedgerRuntimePrerequisiteReport verifyRuntimePrerequisites(Set<LedgerRuntimePrerequisite> availablePrerequisites);
}
