/*
 * 檔案用途：Redis key namespace helper，集中管理 key prefix 與版本化命名。
 */
package com.example.exchange.infra.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyNamespace {

    /** 空字串代表維持舊 key；非空時會加在所有 Redis key 前方。 */
    private final String prefix;

    public RedisKeyNamespace(@Value("${redis-key.prefix:}") String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.prefix = normalized;
    }

    /** 產生單一 key；rawKey 不應包含前導冒號。 */
    public String key(String rawKey) {
        if (prefix.isBlank()) return rawKey;
        return prefix + ":" + rawKey;
    }

    /** 產生 scan/pattern 用 key pattern，套用與 key() 相同的 namespace。 */
    public String pattern(String rawPattern) {
        if (prefix.isBlank()) return rawPattern;
        return prefix + ":" + rawPattern;
    }
}
