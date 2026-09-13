package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.List;
import java.util.UUID;

/**
 * 註冊 email 的雙驗證碼材料。
 *
 * <p>原始驗證碼只會在建立請求後交給 SMTP adapter，絕不可寫入資料庫、log 或 API response；持久層僅保存
 * 兩組 SHA-256 摘要。</p>
 */
public record RegistrationVerificationSecret(
    UUID registrationId,
    String numericCode,
    String letterCode,
    List<String> letterOptions,
    String numericCodeDigest,
    String letterCodeDigest
) {
    public RegistrationVerificationSecret {
        Objects.requireNonNull(registrationId, "registrationId must not be null");
        Objects.requireNonNull(numericCode, "numericCode must not be null");
        Objects.requireNonNull(letterCode, "letterCode must not be null");
        letterOptions = List.copyOf(Objects.requireNonNull(letterOptions, "letterOptions must not be null"));
        Objects.requireNonNull(numericCodeDigest, "numericCodeDigest must not be null");
        Objects.requireNonNull(letterCodeDigest, "letterCodeDigest must not be null");
    }
}
