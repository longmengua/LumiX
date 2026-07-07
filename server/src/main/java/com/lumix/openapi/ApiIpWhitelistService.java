package com.lumix.openapi;

/**
 * Open API IP 白名單契約。
 */
public interface ApiIpWhitelistService {

    // TODO(HUMAN_REVIEW_REQUIRED): 檢查來源 IP 是否允許通過該 API 邊界。
    boolean isAllowed(ApiKeyView apiKeyView, String sourceIp);
}
