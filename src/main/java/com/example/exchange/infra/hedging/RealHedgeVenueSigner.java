/*
 * 檔案用途：real hedge venue adapter skeleton 的 HMAC request signer。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

public class RealHedgeVenueSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String apiKey;
    private final String secret;
    private final Clock clock;

    public RealHedgeVenueSigner(String apiKey, String secret, Clock clock) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("hedge venue api key is required");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("hedge venue secret is required");
        }
        this.apiKey = apiKey.trim();
        this.secret = secret;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public SignedHedgeVenueRequest signSubmit(HedgeOrderRequest request) {
        validate(request);
        String method = "POST";
        String path = "/hedge/orders";
        String payload = payload(request);
        String timestamp = Instant.now(clock).toString();
        String preimage = timestamp + "\n" + method + "\n" + path + "\n" + payload;
        return new SignedHedgeVenueRequest(
                method,
                path,
                payload,
                Map.of(
                        "X-Hedge-Api-Key", apiKey,
                        "X-Hedge-Timestamp", timestamp,
                        "X-Hedge-Signature", hmac(preimage)
                )
        );
    }

    private String hmac(String preimage) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(preimage.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign hedge venue request", e);
        }
    }

    private static void validate(HedgeOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("hedge order request cannot be null");
        }
        if (request.marketMakerId() == null || request.marketMakerId().isBlank()) {
            throw new IllegalArgumentException("market maker id is required");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (request.side() == null) {
            throw new IllegalArgumentException("side is required");
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (request.referencePrice() == null || request.referencePrice().signum() <= 0) {
            throw new IllegalArgumentException("reference price must be positive");
        }
        if (request.limitPrice() == null || request.limitPrice().signum() <= 0) {
            throw new IllegalArgumentException("limit price must be positive");
        }
        if (request.refId() == null || request.refId().isBlank()) {
            throw new IllegalArgumentException("ref id is required");
        }
    }

    private static String payload(HedgeOrderRequest request) {
        return "{"
                + "\"marketMakerId\":\"" + escape(request.marketMakerId().trim()) + "\","
                + "\"uid\":" + request.uid() + ","
                + "\"symbol\":\"" + escape(request.symbol().trim().toUpperCase()) + "\","
                + "\"side\":\"" + request.side().name() + "\","
                + "\"quantity\":\"" + request.quantity().toPlainString() + "\","
                + "\"referencePrice\":\"" + request.referencePrice().toPlainString() + "\","
                + "\"limitPrice\":\"" + request.limitPrice().toPlainString() + "\","
                + "\"refId\":\"" + escape(request.refId().trim()) + "\""
                + "}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
