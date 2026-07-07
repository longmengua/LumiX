package com.lumix.api.validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 單一 validation 錯誤細節。
 *
 * 這個 DTO 的重點是保留可公開的欄位，同時讓 rejectedValue 預設保持保守。
 */
public record ValidationErrorDetail(
    String field,
    String reason,
    String rejectedValue,
    boolean rejectedValuePublic
) {

    public ValidationErrorDetail {
        field = requireText(field, "field");
        reason = requireText(reason, "reason");
        if (rejectedValue != null) {
            rejectedValue = rejectedValue.trim();
            if (rejectedValue.isEmpty()) {
                rejectedValue = null;
            }
        }
        if (!rejectedValuePublic) {
            rejectedValue = null;
        }
    }

    /**
     * 建立預設安全的 validation error detail。
     *
     * 預設不公開 rejectedValue，避免把敏感輸入直接寫進 response。
     */
    public static ValidationErrorDetail safe(String field, String reason) {
        return new ValidationErrorDetail(field, reason, null, false);
    }

    /**
     * 建立明確允許公開 rejectedValue 的 detail。
     *
     * 只有在值已被判定為安全、可公開且不包含敏感資訊時才應使用。
     */
    public static ValidationErrorDetail publicRejectedValue(String field, String reason, String rejectedValue) {
        return new ValidationErrorDetail(field, reason, rejectedValue, true);
    }

    /**
     * 轉成可公開的 map。
     *
     * 這裡刻意使用最小欄位集合，避免 validation response 洩漏多餘資訊。
     */
    public Map<String, Object> toPublicMap() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("field", field);
        detail.put("reason", reason);
        if (rejectedValuePublic && rejectedValue != null) {
            detail.put("rejectedValue", rejectedValue);
        }
        return Map.copyOf(detail);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
