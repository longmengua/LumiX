package com.lumix.openapi;

/**
 * Open API 簽名驗證契約。
 */
public interface ApiSignatureVerifier {

    // TODO(HUMAN_REVIEW_REQUIRED): 驗證 API 簽章；正式版本必須清楚定義 canonical payload 與 key 使用邊界。
    boolean verify(ApiSignatureRequest request, ApiKeyView apiKeyView);
}
