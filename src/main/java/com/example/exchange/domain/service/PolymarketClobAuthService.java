package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketApiCredentialsResponse;
import com.example.exchange.domain.util.PolymarketClobAuthSigner;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketClobAuthService {

    private final ObjectMapper objectMapper;
    private final PolymarketConfigs polymarketConfigs;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .readTimeout(java.time.Duration.ofSeconds(30))
            .writeTimeout(java.time.Duration.ofSeconds(30))
            .build();

    public PolymarketApiCredentialsResponse createApiKey(
            BigInteger nonce
    ) {
        return requestApiCredentials(
                "POST",
                "/auth/api-key",
                nonce
        );
    }

    public PolymarketApiCredentialsResponse deriveApiKey(
            BigInteger nonce
    ) {
        return requestApiCredentials(
                "GET",
                "/auth/derive-api-key",
                nonce
        );
    }

    private PolymarketApiCredentialsResponse requestApiCredentials(
            String method,
            String path,
            BigInteger nonce
    ) {
        BigInteger safeNonce =
                nonce == null ? BigInteger.ZERO : nonce;

        Credentials credentials =
                Credentials.create(resolvePrivateKey());

        String timestamp =
                String.valueOf(Instant.now().getEpochSecond());

        String signature =
                PolymarketClobAuthSigner.sign(
                        polymarketConfigs,
                        credentials,
                        timestamp,
                        safeNonce
                );

        String url =
                polymarketConfigs.getClob().getBaseUrl() + path;

        Request.Builder builder =
                new Request.Builder()
                        .url(url)
                        .addHeader("POLY_ADDRESS", credentials.getAddress())
                        .addHeader("POLY_SIGNATURE", signature)
                        .addHeader("POLY_TIMESTAMP", timestamp)
                        .addHeader("POLY_NONCE", safeNonce.toString());

        if ("POST".equalsIgnoreCase(method)) {
            builder.post(okhttp3.RequestBody.create(new byte[0]));
        } else {
            builder.get();
        }

        try (Response response = okHttpClient.newCall(builder.build()).execute()) {
            String body =
                    response.body() == null
                            ? ""
                            : response.body().string();

            if (!response.isSuccessful()) {
                log.warn(
                        "[PolymarketCLOBAuth] API credential request failed. method={}, path={}, code={}, body={}",
                        method,
                        path,
                        response.code(),
                        body
                );

                return PolymarketApiCredentialsResponse.builder()
                        .success(false)
                        .signerAddress(credentials.getAddress())
                        .nonce(safeNonce.toString())
                        .errorMsg(body)
                        .build();
            }

            Map<String, Object> raw =
                    objectMapper.readValue(
                            body,
                            new TypeReference<Map<String, Object>>() {
                            }
                    );

            return PolymarketApiCredentialsResponse.builder()
                    .success(true)
                    .apiKey(firstNonBlank(raw.get("apiKey"), raw.get("key")))
                    .secret(firstNonBlank(raw.get("secret"), raw.get("apiSecret")))
                    .passphrase(firstNonBlank(raw.get("passphrase"), raw.get("apiPassphrase")))
                    .signerAddress(credentials.getAddress())
                    .nonce(safeNonce.toString())
                    .build();
        } catch (Exception e) {
            log.warn(
                    "[PolymarketCLOBAuth] API credential request exception. method={}, path={}",
                    method,
                    path,
                    e
            );

            return PolymarketApiCredentialsResponse.builder()
                    .success(false)
                    .signerAddress(credentials.getAddress())
                    .nonce(safeNonce.toString())
                    .errorMsg(e.getMessage())
                    .build();
        }
    }

    private String resolvePrivateKey() {
        String privateKey =
                polymarketConfigs
                        .getWallet()
                        .getPrivateKey();

        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalArgumentException(
                    "polymarket.wallet.private-key is required"
            );
        }

        return privateKey;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }

            String text =
                    value.toString();

            if (!text.isBlank()) {
                return text;
            }
        }

        return null;
    }
}
