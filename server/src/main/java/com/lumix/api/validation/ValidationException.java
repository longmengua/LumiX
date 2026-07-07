package com.lumix.api.validation;

import java.util.List;
import java.util.Objects;

/**
 * API validation boundary 的受控例外。
 *
 * 這類例外只攜帶已去敏的 validation detail，方便 handler 統一轉成公開錯誤回應。
 */
public class ValidationException extends RuntimeException {

    private final List<ValidationErrorDetail> violations;

    /**
     * 建立 validation exception。
     *
     * violations 不應為空，否則無法對外提供可執行的修正提示。
     */
    public ValidationException(List<ValidationErrorDetail> violations) {
        this("請求驗證失敗", violations);
    }

    /**
     * 建立帶自訂訊息的 validation exception。
     *
     * 自訂訊息只供內部分類，不應直接拿去當 response message。
     */
    public ValidationException(String message, List<ValidationErrorDetail> violations) {
        super(Objects.requireNonNull(message, "message must not be null"));
        List<ValidationErrorDetail> safeViolations = List.copyOf(Objects.requireNonNull(violations, "violations must not be null"));
        if (safeViolations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        this.violations = safeViolations;
    }

    /**
     * 取得已去敏的 validation violations。
     */
    public List<ValidationErrorDetail> getViolations() {
        return violations;
    }
}
