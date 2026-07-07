package com.lumix.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lumix.common.RequestId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

/**
 * 對外錯誤回應格式。
 *
 * 這個 DTO 要維持穩定且去敏，因為它會直接暴露給 API 使用者與前端。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String code,
    String message,
    String requestId,
    String timestamp,
    Map<String, Object> details
) {

    public ApiErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        details = details == null ? null : Map.copyOf(details);
    }

    /**
     * 建立標準錯誤回應。
     *
     * requestId 是必要的追蹤欄位，方便後續查 log 或 trace。
     */
    public static ApiErrorResponse of(ApiErrorCode errorCode, RequestId requestId, Map<String, Object> details) {
        return new ApiErrorResponse(
            errorCode.getCode(),
            errorCode.getDefaultMessage(),
            requestId.value(),
            OffsetDateTime.now(ZoneOffset.UTC).toString(),
            details
        );
    }
}
