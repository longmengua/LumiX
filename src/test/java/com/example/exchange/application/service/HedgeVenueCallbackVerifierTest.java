/*
 * 檔案用途：測試 hedge venue callback HMAC 簽章驗證。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.infra.config.HedgeVenueCallbackProperties;
import com.example.exchange.interfaces.web.dto.HedgeVenueFillCallbackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HedgeVenueCallbackVerifierTest {

    @Test
    @DisplayName("signatureRequired 關閉時不要求 callback headers")
    void verifySkipsHeadersWhenSignatureNotRequired() {
        HedgeVenueCallbackVerifier verifier =
                new HedgeVenueCallbackVerifier(new HedgeVenueCallbackProperties());

        // 流程：dev/local 預設關閉簽章驗證，既有 callback ingestion 行為維持不變。
        assertThatCode(() -> verifier.verify(request(), null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("signatureRequired 開啟時接受有效 HMAC 簽章")
    void verifyAcceptsValidSignature() {
        HedgeVenueCallbackProperties properties = requiredProperties();
        HedgeVenueCallbackVerifier verifier = new HedgeVenueCallbackVerifier(properties);
        verifier.setClockForTest(Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC));
        String timestamp = "2026-05-30T12:00:00Z";
        String signature = verifier.sign(request(), timestamp, properties.getSignatureSecret());

        // 流程：venue 以 shared secret 簽 timestamp + canonical payload，server 驗證後才寫入 fill audit。
        assertThatCode(() -> verifier.verify(request(), timestamp, "sha256=" + signature))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("signatureRequired 開啟時拒絕錯誤簽章")
    void verifyRejectsInvalidSignature() {
        HedgeVenueCallbackProperties properties = requiredProperties();
        HedgeVenueCallbackVerifier verifier = new HedgeVenueCallbackVerifier(properties);
        verifier.setClockForTest(Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC));

        // 流程：payload/header 有任一處被竄改，HMAC 不相符就拒絕，不進入 idempotent fill append。
        assertThatThrownBy(() -> verifier.verify(request(), "2026-05-30T12:00:00Z", "bad-signature"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature is invalid");
    }

    @Test
    @DisplayName("signatureRequired 開啟時拒絕過期 timestamp")
    void verifyRejectsExpiredTimestamp() {
        HedgeVenueCallbackProperties properties = requiredProperties();
        HedgeVenueCallbackVerifier verifier = new HedgeVenueCallbackVerifier(properties);
        verifier.setClockForTest(Clock.fixed(Instant.parse("2026-05-30T12:10:01Z"), ZoneOffset.UTC));
        String timestamp = "2026-05-30T12:00:00Z";
        String signature = verifier.sign(request(), timestamp, properties.getSignatureSecret());

        // 流程：即使簽章正確，timestamp 超過容忍時間也視為 replay risk 並拒絕。
        assertThatThrownBy(() -> verifier.verify(request(), timestamp, signature))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside tolerance");
    }

    @Test
    @DisplayName("signatureRequired 開啟但 secret 未配置時拒絕啟用")
    void verifyRejectsMissingSecretWhenRequired() {
        HedgeVenueCallbackProperties properties = new HedgeVenueCallbackProperties();
        properties.setSignatureRequired(true);
        HedgeVenueCallbackVerifier verifier = new HedgeVenueCallbackVerifier(properties);

        // 流程：production 開啟驗簽但未注入 secret 時不能默默接受 callback。
        assertThatThrownBy(() -> verifier.verify(request(), "2026-05-30T12:00:00Z", "anything"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secret is not configured");
    }

    private static HedgeVenueCallbackProperties requiredProperties() {
        HedgeVenueCallbackProperties properties = new HedgeVenueCallbackProperties();
        properties.setSignatureRequired(true);
        properties.setSignatureSecret("venue-callback-secret");
        properties.setTimestampToleranceSeconds(300);
        return properties;
    }

    private static HedgeVenueFillCallbackRequest request() {
        return new HedgeVenueFillCallbackRequest(
                "mm-1",
                "BTCUSDT",
                "venue-order-1",
                "venue-fill-1",
                OrderSide.SELL,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                new BigDecimal("0.01"),
                "USDT",
                "hedge-ref-1",
                Instant.parse("2026-05-30T11:59:59Z")
        );
    }
}
