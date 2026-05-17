/*
 * 檔案用途：測試日誌敏感資訊遮罩，避免 private key、API secret 與簽名外洩。
 */
package com.example.exchange.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試敏感資訊遮罩規則，避免 log 洩漏私鑰、API secret、簽名與 token。
 */
class SensitiveLogSanitizerTest {

    @Test
    @DisplayName("遮罩 query string 與 key=value 形式的 secret")
    /**
     * 流程：輸入 URL/query 與 key=value 混合字串 -> sanitize -> 驗證敏感值消失、非敏感欄位保留。
     */
    void masksQueryAndAssignmentStyleSecrets() {
        String raw =
                "/orders?private-key=0xabc&api-secret=s3cr3t&signature=0xsig&signatureType=3&marketSlug=btc";

        String sanitized =
                SensitiveLogSanitizer.sanitize(raw);

        assertThat(sanitized)
                .contains("private-key=***")
                .contains("api-secret=***")
                .contains("signature=***")
                .contains("signatureType=3")
                .contains("marketSlug=btc")
                .doesNotContain("0xabc")
                .doesNotContain("s3cr3t")
                .doesNotContain("0xsig");
    }

    @Test
    @DisplayName("遮罩 JSON body 形式的 API credentials")
    /**
     * 流程：輸入 JSON credentials -> sanitize -> 驗證 apiKey/apiSecret/passphrase 被替換成遮罩。
     */
    void masksJsonStyleSecrets() {
        String raw =
                "{\"apiKey\":\"key-123\",\"apiSecret\":\"secret-123\",\"apiPassphrase\":\"pass-123\",\"order\":\"ok\"}";

        String sanitized =
                SensitiveLogSanitizer.sanitize(raw);

        assertThat(sanitized)
                .contains("\"apiKey\":\"***\"")
                .contains("\"apiSecret\":\"***\"")
                .contains("\"apiPassphrase\":\"***\"")
                .contains("\"order\":\"ok\"")
                .doesNotContain("key-123")
                .doesNotContain("secret-123")
                .doesNotContain("pass-123");
    }

    @Test
    @DisplayName("遮罩 Authorization header value")
    /**
     * 流程：輸入 Authorization header 文字 -> sanitize -> 驗證 bearer token 被移除但其他 path 內容仍在。
     */
    void masksAuthorizationValues() {
        String raw =
                "Authorization: Bearer token-123, path=/api/orders";

        String sanitized =
                SensitiveLogSanitizer.sanitize(raw);

        assertThat(sanitized)
                .contains("Authorization: ***")
                .contains("path=/api/orders")
                .doesNotContain("token-123");
    }

    @Test
    @DisplayName("只遮罩已知敏感 header，保留 request id")
    /**
     * 流程：分別丟敏感 header 與追蹤 header -> 驗證只有敏感 header 被遮罩。
     */
    void masksKnownSensitiveHeadersOnly() {
        assertThat(SensitiveLogSanitizer.sanitizeHeader("POLY_SIGNATURE", "0xsig"))
                .isEqualTo("***");
        assertThat(SensitiveLogSanitizer.sanitizeHeader("X-Request-Id", "request-1"))
                .isEqualTo("request-1");
    }
}
