/*
 * 檔案用途：應用服務，驗證 hedge venue callback 的 HMAC 簽章與 timestamp。
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.HedgeVenueCallbackProperties;
import com.example.exchange.interfaces.web.dto.HedgeVenueFillCallbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class HedgeVenueCallbackVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final HedgeVenueCallbackProperties properties;
    private Clock clock = Clock.systemUTC();

    void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    public void verify(HedgeVenueFillCallbackRequest request, String timestampHeader, String signatureHeader) {
        if (!properties.isSignatureRequired()) {
            return;
        }
        if (properties.getSignatureSecret() == null || properties.getSignatureSecret().isBlank()) {
            throw new IllegalStateException("hedge venue callback signature required but secret is not configured");
        }
        Instant timestamp = parseTimestamp(timestampHeader);
        Duration age = Duration.between(timestamp, Instant.now(clock)).abs();
        if (age.getSeconds() > properties.getTimestampToleranceSeconds()) {
            throw new IllegalArgumentException("hedge venue callback timestamp is outside tolerance");
        }
        String expected = sign(request, timestampHeader, properties.getSignatureSecret());
        if (signatureHeader == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                normalizeSignature(signatureHeader).getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException("hedge venue callback signature is invalid");
        }
    }

    public String sign(HedgeVenueFillCallbackRequest request, String timestampHeader, String secret) {
        if (request == null) {
            throw new IllegalArgumentException("hedge venue callback request is required");
        }
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw new IllegalArgumentException("hedge venue callback timestamp is required");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("hedge venue callback signature secret is required");
        }
        return hmacHex(secret, timestampHeader.trim() + "." + canonicalPayload(request));
    }

    private static Instant parseTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw new IllegalArgumentException("hedge venue callback timestamp is required");
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(timestampHeader.trim()));
        } catch (NumberFormatException ignored) {
            try {
                return Instant.parse(timestampHeader.trim());
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("hedge venue callback timestamp is invalid");
            }
        }
    }

    private static String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign hedge venue callback", ex);
        }
    }

    private static String canonicalPayload(HedgeVenueFillCallbackRequest request) {
        return String.join(
                "\n",
                value(request.marketMakerId()),
                value(request.symbol()),
                value(request.venueOrderId()),
                value(request.venueFillId()),
                value(request.side() == null ? null : request.side().name()),
                value(request.quantity()),
                value(request.price()),
                value(request.fee()),
                value(request.feeAsset()),
                value(request.refId()),
                value(request.filledAt() == null ? null : request.filledAt().toString())
        );
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String value(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String normalizeSignature(String signature) {
        String normalized = signature == null ? "" : signature.trim().toLowerCase();
        return normalized.startsWith("sha256=")
                ? normalized.substring("sha256=".length())
                : normalized;
    }
}
