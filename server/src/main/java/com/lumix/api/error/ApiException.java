package com.lumix.api.error;

import java.util.Map;
import java.util.Objects;

/**
 * API boundary 用的受控例外。
 *
 * 這種例外只攜帶對外公開可接受的錯誤分類與去敏後的 details，
 * 不應拿來包裝 stack trace 或敏感簽章資訊。
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final Map<String, Object> details;

    /**
     * 建立只帶錯誤碼的 API 例外。
     *
     * 這個建構式適合後續 controller 或 application layer 在不需要額外 details 時使用。
     */
    public ApiException(ApiErrorCode errorCode) {
        this(errorCode, null, null);
    }

    /**
     * 建立帶 details 的 API 例外。
     *
     * details 必須是去敏後內容，因為 handler 最終會原樣放進 response。
     */
    public ApiException(ApiErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, null, details);
    }

    /**
     * 建立帶 cause 的 API 例外。
     *
     * cause 只保留在內部追查，對外 response 仍會使用安全的預設 message。
     */
    public ApiException(ApiErrorCode errorCode, Throwable cause, Map<String, Object> details) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.details = details == null ? null : Map.copyOf(details);
    }

    /**
     * 取得對外使用的錯誤碼。
     */
    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 取得已去敏的補充資訊。
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
