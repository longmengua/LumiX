package com.lumix.admin.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdminReadOnlyViewPolicyTest {
    @Test
    void unhealthySourceCannotBePresentedAsOperationalView() {
        // 管理端唯讀資訊必須保留 health 邊界，而非靜默顯示過期投影。
        AdminReadOnlyViewPolicy policy = new AdminReadOnlyViewPolicy();
        assertEquals(AdminReadOnlyViewDecision.SOURCE_UNHEALTHY_REJECTED, policy.evaluate(new AdminReadOnlyViewEvidence("risk-view", "risk", false, Instant.EPOCH)));
        assertEquals(AdminReadOnlyViewDecision.VIEW_AVAILABLE, policy.evaluate(new AdminReadOnlyViewEvidence("risk-view", "risk", true, Instant.EPOCH)));
    }
}
