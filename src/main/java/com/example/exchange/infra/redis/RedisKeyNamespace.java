/*
 * 檔案用途：Redis key namespace helper，集中管理 key prefix 與版本化命名。
 */
package com.example.exchange.infra.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyNamespace {

    private final String prefix;

    public RedisKeyNamespace(@Value("${redis-key.prefix:}") String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.prefix = normalized;
    }

    public String key(String rawKey) {
        if (prefix.isBlank()) return rawKey;
        return prefix + ":" + rawKey;
    }

    public String pattern(String rawPattern) {
        if (prefix.isBlank()) return rawPattern;
        return prefix + ":" + rawPattern;
    }
}
