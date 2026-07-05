package com.lumix.openapi;

/**
 * Open API IP 白名單契約。
 */
public interface ApiIpWhitelistService {

    // TODO: requires high-reasoning review before production use
    boolean isAllowed(ApiKeyView apiKeyView, String sourceIp);
}
