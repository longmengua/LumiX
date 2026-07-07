package com.lumix.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.api.error.ApiExceptionHandler;
import com.lumix.common.RequestId;
import org.junit.jupiter.api.Test;

/**
 * 驗證 security boundary 只做分類，不做登入或權限 runtime。
 */
class SecurityBoundaryTest {

    private final SecurityPolicy policy = new SecurityPolicy();

    /**
     * 確認 read-only query 不會被標成高風險。
     */
    @Test
    void readOnlyQueryIsLowRisk() {
        assertEquals(SecurityRiskLevel.LOW, policy.classify(SecurityOperation.READ_ONLY_QUERY));
        assertFalse(policy.requiresHumanReview(SecurityOperation.READ_ONLY_QUERY));
    }

    /**
     * 確認 withdrawal request 屬於 production-gated 且需要 human review。
     */
    @Test
    void withdrawalRequestIsProductionGated() {
        assertEquals(SecurityRiskLevel.PRODUCTION_GATED, policy.classify(SecurityOperation.WITHDRAWAL_REQUEST));
        assertTrue(policy.requiresHumanReview(SecurityOperation.WITHDRAWAL_REQUEST));
    }

    /**
     * 確認 admin action 不能被當成一般操作。
     */
    @Test
    void adminActionRequiresHumanReview() {
        assertEquals(SecurityRiskLevel.PRODUCTION_GATED, policy.classify(SecurityOperation.ADMIN_ACTION));
        assertTrue(policy.requiresHumanReview(SecurityOperation.ADMIN_ACTION));
    }

    /**
     * 確認 security exception 會被安全轉成 ApiErrorResponse。
     */
    @Test
    void securityExceptionMapsToApiErrorResponse() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ApiErrorResponse response = handler.toResponse(
            new SecurityException(ApiErrorCode.AUTHORIZATION_ERROR, true),
            new RequestId("req-sec-1")
        );

        assertEquals(ApiErrorCode.AUTHORIZATION_ERROR.getCode(), response.code());
        assertEquals(ApiErrorCode.AUTHORIZATION_ERROR.getDefaultMessage(), response.message());
        assertEquals("req-sec-1", response.requestId());
        assertTrue(response.details() == null);
    }
}
