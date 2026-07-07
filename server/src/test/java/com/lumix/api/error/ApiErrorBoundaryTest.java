package com.lumix.api.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.common.BusinessException;
import com.lumix.common.ErrorCode;
import com.lumix.common.RequestId;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 驗證 API error boundary 的回應格式與分類。
 *
 * 這組測試的目的是保護錯誤 contract 不退化成把內部例外直接吐給外部。
 */
class ApiErrorBoundaryTest {

    /**
     * 確認 response schema 具備必要欄位，而且 details 只有在有內容時才出現。
     */
    @Test
    void responseFormatIncludesRequiredFields() throws Exception {
        ApiErrorResponse response = ApiErrorResponse.of(
            ApiErrorCode.VALIDATION_ERROR,
            new RequestId("req-001"),
            Map.of("field", "amount")
        );

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals("VALIDATION_ERROR", jsonNode.get("code").asText());
        assertEquals("請求驗證失敗", jsonNode.get("message").asText());
        assertEquals("req-001", jsonNode.get("requestId").asText());
        assertTrue(jsonNode.has("timestamp"));
        assertEquals("amount", jsonNode.path("details").path("field").asText());
    }

    /**
     * 確認內部 BusinessException 會被保守映射，避免把細節變成新的 API contract。
     */
    @Test
    void businessExceptionMapsToSafeApiErrorCode() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ApiErrorResponse response = handler.toResponse(
            new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED, "same account transfer"),
            new RequestId("req-002")
        );

        assertEquals(ApiErrorCode.CONFLICT.getCode(), response.code());
        assertEquals(ApiErrorCode.CONFLICT.getDefaultMessage(), response.message());
        assertEquals("req-002", response.requestId());
        assertNull(response.details());
    }

    /**
     * 確認高風險操作拒絕保留 HUMAN_REVIEW_REQUIRED 語意，不能被降級成普通錯誤。
     */
    @Test
    void highRiskOperationRejectedKeepsHumanReviewFlag() {
        assertTrue(ApiErrorCode.HIGH_RISK_OPERATION_REJECTED.isHumanReviewRequired());
        assertFalse(ApiErrorCode.INTERNAL_ERROR.isHumanReviewRequired());
    }

    /**
     * 確認高風險 API 例外可以直接保留專用分類，方便後續審核與風控接手。
     */
    @Test
    void apiExceptionPreservesHighRiskClassification() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ApiErrorResponse response = handler.toResponse(
            new ApiException(ApiErrorCode.HIGH_RISK_OPERATION_REJECTED, Map.of("domain", "ledger")),
            new RequestId("req-004")
        );

        assertEquals(ApiErrorCode.HIGH_RISK_OPERATION_REJECTED.getCode(), response.code());
        assertEquals(ApiErrorCode.HIGH_RISK_OPERATION_REJECTED.getDefaultMessage(), response.message());
        assertEquals("req-004", response.requestId());
        assertEquals("ledger", response.details().get("domain"));
    }

    /**
     * 確認未知例外不外洩內部訊息，只回傳安全的 internal error。
     */
    @Test
    void unknownExceptionMapsToInternalErrorWithoutDetailsLeak() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ApiErrorResponse response = handler.toResponse(
            new RuntimeException("SQL should not leak"),
            new RequestId("req-003")
        );

        assertEquals(ApiErrorCode.INTERNAL_ERROR.getCode(), response.code());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.getDefaultMessage(), response.message());
        assertEquals("req-003", response.requestId());
        assertNull(response.details());
    }
}
