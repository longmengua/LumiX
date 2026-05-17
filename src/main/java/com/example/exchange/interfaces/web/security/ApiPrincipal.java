/*
 * 檔案用途：Web 安全模型，表示已通過 API key 或 JWT 驗證的呼叫者。
 */
package com.example.exchange.interfaces.web.security;

import java.util.Set;

public record ApiPrincipal(
        String subject,
        String credentialType,
        Set<String> roles,
        Set<String> scopes
) {
}
