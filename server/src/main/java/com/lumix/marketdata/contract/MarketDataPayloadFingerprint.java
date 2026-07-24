package com.lumix.marketdata.contract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 由固定 canonical payload form 計算 identity 所需的 SHA-256 fingerprint。
 *
 * <p>canonical form 使用明確長度前綴且不讀取 clock，避免 delimiter、locale 或集合雜湊順序改變 event identity。</p>
 */
final class MarketDataPayloadFingerprint {

    private MarketDataPayloadFingerprint() {
    }

    static String forPayload(MarketDataPayload payload) {
        MarketDataContractValidation.requireValue(payload, "payload");
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(payload.canonicalForm().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format(java.util.Locale.ROOT, "%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 是 Java 平台必要演算法；若缺失，不能產生不可靠的替代 identity。
            throw new IllegalStateException("SHA-256 is required for normalized market-data identity", exception);
        }
    }
}
