package com.lumix.admin.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.UserId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminAuthorizationPolicyTest {
    @Test
    void expiredMfaMissingWrongRoleAndSelfReviewFailClosed() {
        // 管理端任何 session 或職責分離缺口都不可形成可執行 action。
        Instant now = Instant.EPOCH;
        AdminAuthorizationPolicy policy = new AdminAuthorizationPolicy();
        assertEquals(AdminAuthorizationDecision.SESSION_EXPIRED_REJECTED, policy.evaluate(new AdminSessionEvidence(new UserId("a"), Set.of(AdminRole.READ_ONLY_OPERATOR), true, now), AdminOperation.READ_OPERATIONAL_VIEW, now));
        assertEquals(AdminAuthorizationDecision.MFA_REQUIRED_REJECTED, policy.evaluate(new AdminSessionEvidence(new UserId("a"), Set.of(AdminRole.READ_ONLY_OPERATOR), false, now.plusSeconds(1)), AdminOperation.READ_OPERATIONAL_VIEW, now));
        assertEquals(AdminAuthorizationDecision.ROLE_NOT_ALLOWED_REJECTED, policy.evaluate(new AdminSessionEvidence(new UserId("a"), Set.of(AdminRole.READ_ONLY_OPERATOR), true, now.plusSeconds(1)), AdminOperation.REQUEST_CONTROLLED_ACTION, now));
        assertThrows(IllegalArgumentException.class, () -> new AdminControlledActionEvidence("x", new UserId("a"), new UserId("a"), "reason", now));
    }
}
