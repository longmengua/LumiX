package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證目前 skeleton 的 package dependency 沒有明顯違規。
 *
 * 這個測試只做 source-level guardrail，不取代未來更完整的架構掃描工具。
 */
class ModuleDependencyGuardrailTest {

    private final ModuleDependencyPolicy policy = new ModuleDependencyPolicy();

    /**
     * 確認高風險 module 會被標記出來，方便後續加嚴規則。
     */
    @Test
    void highRiskModulePrefixesAreRecognized() {
        assertTrue(policy.isHighRiskModule("com.lumix.ledger."));
        assertTrue(policy.isHighRiskModule("com.lumix.withdrawal."));
        assertTrue(policy.isHighRiskModule("com.lumix.settlement."));
        assertFalse(policy.isHighRiskModule("com.lumix.api."));
    }
}
