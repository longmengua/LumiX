/*
 * 檔案用途：測試 real hedge venue adapter skeleton 的安全停用與簽名 request contract。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.enums.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RealHedgeVenueAdapterTest {

    @Test
    @DisplayName("disabled real hedge venue adapter 會安全拒絕送單")
    void disabledAdapterRejectsSubmitSafely() {
        RealHedgeVenueAdapter adapter = new RealHedgeVenueAdapter(false, null);

        // 場景：即使 class 已存在，未顯式啟用與設定 signer 前，production 不會誤送外部 venue。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isFalse();
        assertThat(result.retryable()).isFalse();
        assertThat(result.reason()).isEqualTo("REAL_HEDGE_VENUE_DISABLED");
    }

    @Test
    @DisplayName("signSubmit 產生穩定 payload、timestamp 與 HMAC headers")
    void signerBuildsDeterministicSignedRequest() {
        RealHedgeVenueSigner signer = new RealHedgeVenueSigner(
                "api-key-1",
                "secret-1",
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：real venue adapter 接線前，先固定簽名 preimage contract，避免後續 HTTP client 實作漂移。
        SignedHedgeVenueRequest signed = signer.signSubmit(request());

        assertThat(signed.method()).isEqualTo("POST");
        assertThat(signed.path()).isEqualTo("/hedge/orders");
        assertThat(signed.payload()).isEqualTo("{\"marketMakerId\":\"mm-1\",\"uid\":9101,\"symbol\":\"BTCUSDT\",\"side\":\"SELL\",\"quantity\":\"1.250\",\"referencePrice\":\"30000.00\",\"limitPrice\":\"29900.00\",\"refId\":\"hedge-ref-1\"}");
        assertThat(signed.headers()).containsEntry("X-Hedge-Api-Key", "api-key-1");
        assertThat(signed.headers()).containsEntry("X-Hedge-Timestamp", "2026-06-01T00:00:00Z");
        assertThat(signed.headers().get("X-Hedge-Signature")).hasSize(64);
    }

    private static HedgeOrderRequest request() {
        return new HedgeOrderRequest(
                "mm-1",
                9101,
                "BTCUSDT",
                OrderSide.SELL,
                new BigDecimal("1.250"),
                new BigDecimal("30000.00"),
                new BigDecimal("29900.00"),
                "hedge-ref-1"
        );
    }
}
