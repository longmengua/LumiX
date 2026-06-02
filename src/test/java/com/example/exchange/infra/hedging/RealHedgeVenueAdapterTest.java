/*
 * 檔案用途：測試 real hedge venue adapter skeleton 的安全停用與簽名 request contract。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.enums.OrderSide;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    @DisplayName("signLookup 產生查詢 ref id 的穩定簽名 request")
    void signerBuildsDeterministicLookupRequest() {
        RealHedgeVenueSigner signer = new RealHedgeVenueSigner(
                "api-key-1",
                "secret-1",
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：補單查詢要用 ref id 做 idempotent lookup，且與 submit 使用同一套 HMAC contract。
        SignedHedgeVenueRequest signed = signer.signLookup("hedge/ref-1");

        assertThat(signed.method()).isEqualTo("GET");
        assertThat(signed.path()).isEqualTo("/hedge/orders/hedge%2Fref-1");
        assertThat(signed.payload()).isEmpty();
        assertThat(signed.headers()).containsEntry("X-Hedge-Api-Key", "api-key-1");
        assertThat(signed.headers()).containsEntry("X-Hedge-Timestamp", "2026-06-01T00:00:00Z");
        assertThat(signed.headers().get("X-Hedge-Signature")).hasSize(64);
    }

    @Test
    @DisplayName("real hedge venue lookup adapter 未設定 HTTP client 前只回傳 retryable 狀態")
    void lookupAdapterHasSafeSkeletonContract() {
        RealHedgeVenueOrderLookupAdapter disabled = new RealHedgeVenueOrderLookupAdapter(false, null);
        RealHedgeVenueOrderLookupAdapter missingSigner = new RealHedgeVenueOrderLookupAdapter(true, null);
        RealHedgeVenueOrderLookupAdapter enabled = new RealHedgeVenueOrderLookupAdapter(
                true,
                new RealHedgeVenueSigner(
                        "api-key-1",
                        "secret-1",
                        Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
                )
        );

        // 場景：production 預設停用不出站；啟用但未設定 HTTP client 時，對帳流程可重試而不誤判成功。
        assertThat(disabled.lookupByRefId("hedge-ref-1")).isEmpty();
        assertThat(missingSigner.lookupByRefId("hedge-ref-1")).get()
                .extracting("reason", "retryable")
                .containsExactly("REAL_HEDGE_VENUE_LOOKUP_SIGNER_NOT_CONFIGURED", true);
        assertThat(enabled.lookupByRefId("hedge-ref-1")).get()
                .extracting("reason", "retryable")
                .containsExactly("REAL_HEDGE_VENUE_LOOKUP_HTTP_NOT_CONFIGURED", true);
    }

    @Test
    @DisplayName("real hedge venue submit 送出 signed HTTP request 並解析 accepted response")
    void submitSendsSignedHttpRequestAndMapsAcceptedResponse() {
        List<okhttp3.Request> seen = new ArrayList<>();
        OkHttpClient client = clientReturning(seen, 200, "{\"accepted\":true,\"venueOrderId\":\"venue-1\"}");
        RealHedgeVenueAdapter adapter = new RealHedgeVenueAdapter(
                true,
                signer(),
                client,
                "https://hedge.example.test/",
                new ObjectMapper()
        );

        // 場景：effectful submit 必須帶簽名 header 與 idempotency key，讓共用 OkHttp retry 不會造成重複外部效果。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isTrue();
        assertThat(result.venueOrderId()).isEqualTo("venue-1");
        assertThat(seen).hasSize(1);
        okhttp3.Request httpRequest = seen.getFirst();
        assertThat(httpRequest.method()).isEqualTo("POST");
        assertThat(httpRequest.url().toString()).isEqualTo("https://hedge.example.test/hedge/orders");
        assertThat(httpRequest.header("X-Hedge-Api-Key")).isEqualTo("api-key-1");
        assertThat(httpRequest.header("X-Hedge-Timestamp")).isEqualTo("2026-06-01T00:00:00Z");
        assertThat(httpRequest.header("X-Hedge-Signature")).hasSize(64);
        assertThat(httpRequest.header("Idempotency-Key")).isEqualTo("hedge-ref-1");
        assertThat(httpRequest.header("X-Idempotency-Key")).isEqualTo("hedge-ref-1");
    }

    @Test
    @DisplayName("real hedge venue lookup 以 signed GET 查詢 ref id 並解析 venue order id")
    void lookupUsesSignedHttpRequestAndMapsAcceptedResponse() {
        List<okhttp3.Request> seen = new ArrayList<>();
        OkHttpClient client = clientReturning(seen, 200, "{\"status\":\"OPEN\",\"orderId\":\"venue-lookup-1\"}");
        RealHedgeVenueOrderLookupAdapter adapter = new RealHedgeVenueOrderLookupAdapter(
                true,
                signer(),
                client,
                "https://hedge.example.test",
                new ObjectMapper()
        );

        // 場景：uncertain submit reconciliation 依 ref id 查 venue，成功命中後回填 terminal venue result。
        HedgeOrderResult result = adapter.lookupByRefId("hedge/ref-1").orElseThrow();

        assertThat(result.accepted()).isTrue();
        assertThat(result.venueOrderId()).isEqualTo("venue-lookup-1");
        assertThat(seen).hasSize(1);
        okhttp3.Request httpRequest = seen.getFirst();
        assertThat(httpRequest.method()).isEqualTo("GET");
        assertThat(httpRequest.url().toString()).isEqualTo("https://hedge.example.test/hedge/orders/hedge%2Fref-1");
        assertThat(httpRequest.header("X-Hedge-Api-Key")).isEqualTo("api-key-1");
    }

    @Test
    @DisplayName("real hedge venue HTTP 5xx 會映射成 retryable rejection")
    void submitMapsServerErrorToRetryableRejection() {
        RealHedgeVenueAdapter adapter = new RealHedgeVenueAdapter(
                true,
                signer(),
                clientReturning(new ArrayList<>(), 503, "{\"reason\":\"venue maintenance\"}"),
                "https://hedge.example.test",
                new ObjectMapper()
        );

        // 場景：外部 venue 暫時錯誤不能誤標 final rejection，後續 idempotency reconcile 仍可查未解 outcome。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isFalse();
        assertThat(result.retryable()).isTrue();
        assertThat(result.reason()).isEqualTo("venue maintenance");
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

    private static RealHedgeVenueSigner signer() {
        return new RealHedgeVenueSigner(
                "api-key-1",
                "secret-1",
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static OkHttpClient clientReturning(List<okhttp3.Request> seen, int statusCode, String body) {
        return new OkHttpClient.Builder()
                .addInterceptor(new StaticResponseInterceptor(seen, statusCode, body))
                .build();
    }

    private record StaticResponseInterceptor(
            List<okhttp3.Request> seen,
            int statusCode,
            String body
    ) implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            seen.add(chain.request());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message("test")
                    .body(ResponseBody.create(body, okhttp3.MediaType.get("application/json; charset=utf-8")))
                    .build();
        }
    }
}
