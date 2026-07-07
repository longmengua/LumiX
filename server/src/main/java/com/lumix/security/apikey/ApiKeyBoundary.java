package com.lumix.security.apikey;

/**
 * API key boundary marker。
 *
 * 這個標記只代表 API key / signature / whitelist / rate limit 的邊界存在，
 * 不代表 runtime 驗證流程已實作。
 */
public interface ApiKeyBoundary {
}
