package com.lumix.security;

import com.lumix.api.error.ApiErrorCode;
import java.util.Objects;

/**
 * security boundary 的受控例外。
 *
 * 這個例外只攜帶對外可公開的安全錯誤分類，不應包含 secret 或 signature payload。
 */
public class SecurityException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final boolean humanReviewRequired;

    /**
     * 建立 security exception。
     *
     * 例外訊息保持安全且短小，避免外洩內部驗證細節。
     */
    public SecurityException(ApiErrorCode errorCode, boolean humanReviewRequired) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getDefaultMessage());
        this.errorCode = errorCode;
        this.humanReviewRequired = humanReviewRequired;
    }

    /**
     * 取得對外錯誤碼。
     */
    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 標示這個 security 事件是否需要 human review。
     */
    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }
}
