package com.lumix.api.error;

import com.lumix.common.BusinessException;
import com.lumix.common.RequestId;
import com.lumix.api.validation.ValidationException;
import com.lumix.persistence.PersistenceErrorCode;
import com.lumix.persistence.PersistenceException;
import java.util.Map;

/**
 * API exception boundary 的骨架。
 *
 * 後續真正的 controller advice 或 web adapter 可以直接委派到這裡，
 * 讓錯誤分類、去敏與 response 格式維持一致。
 */
public class ApiExceptionHandler {

    /**
     * 將任何可預期或不可預期的例外，轉成統一的 API error response。
     *
     * 這裡不回傳 stack trace，也不把內部訊息原封不動外露，避免洩漏 SQL 或簽章資訊。
     */
    public ApiErrorResponse toResponse(Throwable exception, RequestId requestId) {
        if (exception instanceof ApiException apiException) {
            return ApiErrorResponse.of(apiException.getErrorCode(), requestId, apiException.getDetails());
        }

        if (exception instanceof BusinessException businessException) {
            return ApiErrorResponse.of(
                ApiErrorCode.fromCommonErrorCode(businessException.getErrorCode()),
                requestId,
                null
            );
        }

        if (exception instanceof IllegalArgumentException) {
            return ApiErrorResponse.of(ApiErrorCode.VALIDATION_ERROR, requestId, null);
        }

        if (exception instanceof ValidationException validationException) {
            return ApiErrorResponse.of(
                ApiErrorCode.VALIDATION_ERROR,
                requestId,
                Map.of(
                    "violations",
                    validationException.getViolations().stream()
                        .map(violation -> violation.toPublicMap())
                        .toList()
                )
            );
        }

        if (exception instanceof java.util.NoSuchElementException) {
            return ApiErrorResponse.of(ApiErrorCode.NOT_FOUND, requestId, null);
        }

        if (exception instanceof PersistenceException persistenceException) {
            return ApiErrorResponse.of(mapPersistenceErrorCode(persistenceException.getErrorCode()), requestId, null);
        }

        if (exception instanceof IllegalStateException) {
            return ApiErrorResponse.of(ApiErrorCode.CONFLICT, requestId, null);
        }

        return ApiErrorResponse.of(ApiErrorCode.INTERNAL_ERROR, requestId, null);
    }

    /**
     * 將 persistence error code 保守映射成 API error code。
     *
     * 這層只做安全分類，不把底層 SQL / 連線訊息帶出去。
     */
    private ApiErrorCode mapPersistenceErrorCode(PersistenceErrorCode errorCode) {
        return switch (errorCode) {
            case CONSTRAINT_VIOLATION -> ApiErrorCode.CONFLICT;
            case NOT_FOUND -> ApiErrorCode.NOT_FOUND;
            case CONNECTION_FAILURE, QUERY_FAILURE, UNKNOWN -> ApiErrorCode.INTERNAL_ERROR;
        };
    }
}
