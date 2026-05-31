/*
 * 檔案用途：real hedge venue adapter skeleton 的簽名 HTTP request contract。
 */
package com.example.exchange.infra.hedging;

import java.util.Map;

public record SignedHedgeVenueRequest(
        String method,
        String path,
        String payload,
        Map<String, String> headers
) {
    public SignedHedgeVenueRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
