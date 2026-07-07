package com.lumix.api.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.api.error.ApiExceptionHandler;
import com.lumix.common.RequestId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 驗證 validation boundary 只回傳安全欄位，不把敏感 rejectedValue 外洩。
 */
class ValidationBoundaryTest {

    /**
     * 確認預設 detail 不會攜帶 rejectedValue。
     */
    @Test
    void safeValidationDetailOmitsRejectedValue() {
        ValidationErrorDetail detail = ValidationErrorDetail.safe("amount", "must be greater than zero");

        Map<String, Object> publicMap = detail.toPublicMap();

        assertEquals("amount", publicMap.get("field"));
        assertEquals("must be greater than zero", publicMap.get("reason"));
        assertFalse(publicMap.containsKey("rejectedValue"));
    }

    /**
     * 確認即使 detail 類型允許公開 rejectedValue，非公開路徑仍然不會帶出敏感值。
     */
    @Test
    void rejectedValueIsRedactedByDefault() {
        ValidationErrorDetail detail = new ValidationErrorDetail(
            "address",
            "format is invalid",
            "0xdeadbeefprivate",
            false
        );

        Map<String, Object> publicMap = detail.toPublicMap();

        assertEquals("address", publicMap.get("field"));
        assertEquals("format is invalid", publicMap.get("reason"));
        assertFalse(publicMap.containsKey("rejectedValue"));
        assertNull(detail.rejectedValue());
    }

    /**
     * 確認 validation exception 會被轉成安全的 VALIDATION_ERROR。
     */
    @Test
    void validationExceptionMapsToSafeValidationResponse() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ValidationException exception = new ValidationException(
            List.of(
                ValidationErrorDetail.safe("amount", "must be greater than zero"),
                new ValidationErrorDetail("address", "format is invalid", "0xdeadbeefprivate", false)
            )
        );

        ApiErrorResponse response = handler.toResponse(exception, new RequestId("req-010"));

        assertEquals(ApiErrorCode.VALIDATION_ERROR.getCode(), response.code());
        assertEquals(ApiErrorCode.VALIDATION_ERROR.getDefaultMessage(), response.message());
        assertEquals("req-010", response.requestId());
        assertTrue(response.details().containsKey("violations"));
        List<?> violations = (List<?>) response.details().get("violations");
        assertEquals(2, violations.size());
        Map<?, ?> firstViolation = (Map<?, ?>) violations.get(0);
        Map<?, ?> secondViolation = (Map<?, ?>) violations.get(1);
        assertEquals("amount", firstViolation.get("field"));
        assertEquals("must be greater than zero", firstViolation.get("reason"));
        assertEquals("address", secondViolation.get("field"));
        assertEquals("format is invalid", secondViolation.get("reason"));
        assertFalse(secondViolation.containsKey("rejectedValue"));
    }
}
