/*
 * 檔案用途：領域工具，封裝簽名、JSON 處理或文字解析等純技術細節。
 */
package com.example.exchange.domain.util;

import com.example.exchange.infra.config.PolymarketConfigs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class PolymarketL2AuthSigner {
    public static Map<String, String> sign(
            PolymarketConfigs polymarketConfigs,
            String polygonSignerAddress,
            String method,
            String requestPath,
            String body
    ) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        String payload = timestamp
                + method.toUpperCase()
                + requestPath
                + (body == null ? "" : body);

        String signature = hmacSha256Base64(
                polymarketConfigs.getClob().getApiSecret(),
                payload
        );

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("POLY_ADDRESS", polygonSignerAddress);
        headers.put("POLY_SIGNATURE", signature);
        headers.put("POLY_TIMESTAMP", timestamp);
        headers.put("POLY_API_KEY", polymarketConfigs.getClob().getApiKey());
        headers.put("POLY_PASSPHRASE", polymarketConfigs.getClob().getApiPassphrase());

        return headers;
    }

    private static String hmacSha256Base64(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    decodeSecret(secret),
                    "HmacSHA256"
            );
            mac.init(keySpec);

            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Sign Polymarket L2 auth failed", e);
        }
    }

    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("clob apiSecret is required");
        }

        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
