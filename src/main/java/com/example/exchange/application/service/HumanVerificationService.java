/*
 * File purpose: Verify public registration human challenge tokens before account creation.
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.CustomerAuthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HumanVerificationService {

    private final CustomerAuthProperties properties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    /** Registration is fail-closed when captcha is enabled and no valid token/provider response exists. */
    public void verifyRegistration(String token) {
        CustomerAuthProperties.Captcha captcha = properties.getCaptcha();
        if (!captcha.isEnabled()) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("human verification is required");
        }
        if (!captcha.getDevBypassToken().isBlank() && captcha.getDevBypassToken().equals(token)) {
            return;
        }
        if (captcha.getSecret() == null || captcha.getSecret().isBlank()) {
            throw new IllegalStateException("human verification secret is not configured");
        }
        if (!"turnstile".equalsIgnoreCase(captcha.getProvider())) {
            throw new IllegalStateException("human verification provider is not supported");
        }
        if (!verifyTurnstile(token, captcha)) {
            throw new IllegalArgumentException("human verification failed");
        }
    }

    private boolean verifyTurnstile(String token, CustomerAuthProperties.Captcha captcha) {
        RequestBody body = new FormBody.Builder()
                .add("secret", captcha.getSecret())
                .add("response", token)
                .build();
        Request request = new Request.Builder()
                .url(captcha.getVerifyUrl())
                .post(body)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String payload = response.body() == null ? "" : response.body().string();
            JsonNode root = objectMapper.readTree(payload);
            return response.isSuccessful() && root.path("success").asBoolean(false);
        } catch (Exception ex) {
            return false;
        }
    }
}
