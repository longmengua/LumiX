/*
 * 檔案用途：執行 real hedge venue signed HTTP request，並將外部回應映射成內部 hedge result。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

final class RealHedgeVenueHttpClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    RealHedgeVenueHttpClient(OkHttpClient httpClient, ObjectMapper objectMapper, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    boolean configured() {
        return httpClient != null && baseUrl != null;
    }

    HedgeOrderResult submit(SignedHedgeVenueRequest signed, String refId) {
        if (!configured()) {
            return HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_HTTP_NOT_CONFIGURED");
        }
        Request request = requestBuilder(signed)
                .header("Idempotency-Key", refId)
                .header("X-Idempotency-Key", refId)
                .post(RequestBody.create(signed.payload(), JSON))
                .build();
        return executeForResult(request);
    }

    Optional<HedgeOrderResult> lookup(SignedHedgeVenueRequest signed) {
        if (!configured()) {
            return Optional.of(HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_LOOKUP_HTTP_NOT_CONFIGURED"));
        }
        Request request = requestBuilder(signed).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) {
                return Optional.empty();
            }
            return Optional.of(mapResponse(response));
        } catch (IOException ex) {
            return Optional.of(HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_LOOKUP_HTTP_ERROR"));
        }
    }

    private HedgeOrderResult executeForResult(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            return mapResponse(response);
        } catch (IOException ex) {
            return HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_HTTP_ERROR");
        }
    }

    private HedgeOrderResult mapResponse(Response response) throws IOException {
        String body = response.body() == null ? "" : response.body().string();
        if (!response.isSuccessful()) {
            String reason = readReason(body).orElse("REAL_HEDGE_VENUE_HTTP_" + response.code());
            return retryableStatus(response.code())
                    ? HedgeOrderResult.retryableRejected(reason)
                    : HedgeOrderResult.rejected(reason);
        }
        return mapSuccess(body);
    }

    private HedgeOrderResult mapSuccess(String body) {
        Optional<JsonNode> json = readJson(body);
        if (json.isEmpty()) {
            return HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_EMPTY_RESPONSE");
        }
        JsonNode root = json.get();
        String venueOrderId = text(root, "venueOrderId")
                .or(() -> text(root, "orderId"))
                .orElse(null);
        boolean accepted = root.path("accepted").asBoolean(false) || acceptedStatus(root);
        if (accepted && venueOrderId != null && !venueOrderId.isBlank()) {
            return HedgeOrderResult.accepted(venueOrderId);
        }
        String reason = text(root, "reason")
                .or(() -> text(root, "message"))
                .orElse("REAL_HEDGE_VENUE_REJECTED");
        boolean retryable = root.path("retryable").asBoolean(false);
        return retryable ? HedgeOrderResult.retryableRejected(reason) : HedgeOrderResult.rejected(reason);
    }

    private Request.Builder requestBuilder(SignedHedgeVenueRequest signed) {
        Request.Builder builder = new Request.Builder().url(baseUrl + signed.path());
        signed.headers().forEach(builder::header);
        return builder;
    }

    private Optional<String> readReason(String body) {
        return readJson(body)
                .flatMap(root -> text(root, "reason").or(() -> text(root, "message")));
    }

    private Optional<JsonNode> readJson(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readTree(body));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static boolean acceptedStatus(JsonNode root) {
        return text(root, "status")
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(status -> status.equals("ACCEPTED") || status.equals("OPEN") || status.equals("FILLED"))
                .isPresent();
    }

    private static Optional<String> text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        String text = value.asText();
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private static boolean retryableStatus(int code) {
        return code == 408 || code == 409 || code == 425 || code == 429 || code >= 500;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
