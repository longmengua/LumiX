package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.util.PolymarketL2AuthSigner;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolymarketClobTradingClient {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final ObjectMapper objectMapper;
    private final PolymarketConfigs polymarketConfigs;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .readTimeout(java.time.Duration.ofSeconds(30))
            .writeTimeout(java.time.Duration.ofSeconds(30))
            .build();

    public PolymarketPlaceOrderResponse postOrder(
            PolymarketClobOrderRequest clobOrderRequest
    ) {
        String path = "/order";
        String url = polymarketConfigs.getClob().getBaseUrl() + path;

        try {
            String body = objectMapper.writeValueAsString(clobOrderRequest);

            log.info(
                    "[PolymarketCLOB] Start post order. url={}, owner={}, orderType={}, deferExec={}, tokenId={}, side={}, maker={}, signer={}, makerAmount={}, takerAmount={}, signatureType={}, signaturePrefix={}",
                    url,
                    clobOrderRequest.getOwner(),
                    clobOrderRequest.getOrderType(),
                    clobOrderRequest.getDeferExec(),
                    safeGetTokenId(clobOrderRequest),
                    safeGetSide(clobOrderRequest),
                    safeGetMaker(clobOrderRequest),
                    safeGetSigner(clobOrderRequest),
                    safeGetMakerAmount(clobOrderRequest),
                    safeGetTakerAmount(clobOrderRequest),
                    safeGetSignatureType(clobOrderRequest),
                    maskSignature(safeGetSignature(clobOrderRequest))
            );

            Map<String, String> authHeaders = PolymarketL2AuthSigner.sign(
                    polymarketConfigs,
                    "POST",
                    path,
                    body
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, JSON))
                    .addHeader("Content-Type", "application/json");

            authHeaders.forEach(requestBuilder::addHeader);

            try (Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
                String responseBody = response.body() == null
                        ? ""
                        : response.body().string();

                log.info(
                        "[PolymarketCLOB] Post order response. httpCode={}, successful={}, body={}",
                        response.code(),
                        response.isSuccessful(),
                        responseBody
                );

                Map<String, Object> raw = parseJsonToMap(responseBody);

                if (!response.isSuccessful()) {
                    String readableError = classifyClobError(responseBody);

                    log.warn(
                            "[PolymarketCLOB] Post order failed. httpCode={}, reason={}, tokenId={}, side={}, makerAmount={}, takerAmount={}, body={}",
                            response.code(),
                            readableError,
                            safeGetTokenId(clobOrderRequest),
                            safeGetSide(clobOrderRequest),
                            safeGetMakerAmount(clobOrderRequest),
                            safeGetTakerAmount(clobOrderRequest),
                            responseBody
                    );

                    return PolymarketPlaceOrderResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .errorMsg(readableError + " | raw=" + responseBody)
                            .build();
                }

                String orderId = firstNonNullAsString(
                        raw.get("orderID"),
                        raw.get("orderId"),
                        raw.get("id")
                );

                String status = firstNonNullAsString(
                        raw.get("status"),
                        "ACCEPTED"
                );

                log.info(
                        "[PolymarketCLOB] Post order success. orderId={}, status={}, tokenId={}, side={}",
                        orderId,
                        status,
                        safeGetTokenId(clobOrderRequest),
                        safeGetSide(clobOrderRequest)
                );

                return PolymarketPlaceOrderResponse.builder()
                        .success(true)
                        .clobOrderId(orderId)
                        .status(status)
                        .build();
            }
        } catch (Exception e) {
            log.error(
                    "[PolymarketCLOB] Post order exception. url={}, tokenId={}, side={}, makerAmount={}, takerAmount={}",
                    url,
                    safeGetTokenId(clobOrderRequest),
                    safeGetSide(clobOrderRequest),
                    safeGetMakerAmount(clobOrderRequest),
                    safeGetTakerAmount(clobOrderRequest),
                    e
            );

            return PolymarketPlaceOrderResponse.builder()
                    .success(false)
                    .status("EXCEPTION")
                    .errorMsg(e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> parseJsonToMap(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(
                    responseBody,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (Exception e) {
            log.warn(
                    "[PolymarketCLOB] Response body is not valid JSON. body={}",
                    responseBody
            );
            return Map.of();
        }
    }

    private String classifyClobError(String body) {
        if (body == null || body.isBlank()) {
            return "UNKNOWN_ERROR: empty response body";
        }

        String lower = body.toLowerCase();

        if (lower.contains("invalid_signature")
                || lower.contains("invalid signature")
                || lower.contains("signature")) {
            return "INVALID_SIGNATURE: check EIP712 schema, signatureType, exchange contract, maker/signer/funder";
        }

        if (lower.contains("allowance")
                || lower.contains("not enough allowance")
                || lower.contains("insufficient allowance")) {
            return "INSUFFICIENT_ALLOWANCE: wallet has not approved Exchange / NegRiskExchange";
        }

        if (lower.contains("balance")
                || lower.contains("insufficient funds")
                || lower.contains("not enough balance")) {
            return "INSUFFICIENT_BALANCE: wallet balance is not enough";
        }

        if (lower.contains("tick")) {
            return "INVALID_TICK_SIZE: price precision is not accepted";
        }

        if (lower.contains("price")) {
            return "INVALID_PRICE: price is invalid or outside accepted range";
        }

        if (lower.contains("size")
                || lower.contains("amount")
                || lower.contains("makeramount")
                || lower.contains("takeramount")) {
            return "INVALID_AMOUNT: makerAmount / takerAmount / size may be wrong";
        }

        if (lower.contains("unauthorized")
                || lower.contains("auth")
                || lower.contains("api key")
                || lower.contains("api-key")
                || lower.contains("passphrase")) {
            return "AUTH_ERROR: check api-key, api-secret, api-passphrase, POLY headers";
        }

        if (lower.contains("closed")
                || lower.contains("inactive")
                || lower.contains("not active")) {
            return "MARKET_CLOSED_OR_INACTIVE";
        }

        if (lower.contains("minimum")
                || lower.contains("min")) {
            return "BELOW_MIN_ORDER_SIZE";
        }

        return "UNKNOWN_CLOB_ERROR";
    }

    private String firstNonNullAsString(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private String maskSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            return "EMPTY";
        }

        if (signature.length() <= 18) {
            return signature;
        }

        return signature.substring(0, 10)
                + "..."
                + signature.substring(signature.length() - 6);
    }

    private String safeGetTokenId(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getTokenId();
    }

    private String safeGetSide(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getSide();
    }

    private String safeGetMaker(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getMaker();
    }

    private String safeGetSigner(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getSigner();
    }

    private String safeGetMakerAmount(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getMakerAmount();
    }

    private String safeGetTakerAmount(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getTakerAmount();
    }

    private Integer safeGetSignatureType(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getSignatureType();
    }

    private String safeGetSignature(PolymarketClobOrderRequest request) {
        if (request == null || request.getOrder() == null) {
            return null;
        }
        return request.getOrder().getSignature();
    }
}