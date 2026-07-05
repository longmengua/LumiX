package com.lumix.openapi;

/**
 * Open API 簽名驗證契約。
 */
public interface ApiSignatureVerifier {

    // TODO: requires high-reasoning review before production use
    boolean verify(ApiSignatureRequest request, ApiKeyView apiKeyView);
}
