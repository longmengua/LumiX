/*
 * File purpose: Tests for free human-verification registration gates.
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.CustomerAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HumanVerificationServiceTest {

    @Test
    @DisplayName("disabled human verification lets local MVP registration continue")
    void disabledHumanVerificationAllowsRegistration() {
        CustomerAuthProperties properties = new CustomerAuthProperties();
        HumanVerificationService service = new HumanVerificationService(properties, new OkHttpClient(), new ObjectMapper());

        // Scenario: local demos keep captcha disabled unless the operator explicitly enables it.
        assertThatCode(() -> service.verifyRegistration(""))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("enabled human verification accepts an explicit dev bypass token")
    void enabledHumanVerificationAcceptsDevBypassToken() {
        CustomerAuthProperties properties = new CustomerAuthProperties();
        properties.getCaptcha().setEnabled(true);
        properties.getCaptcha().setDevBypassToken("dev-only-token");
        HumanVerificationService service = new HumanVerificationService(properties, new OkHttpClient(), new ObjectMapper());

        // Scenario: automated local tests can use a configured bypass without calling an external captcha provider.
        assertThatCode(() -> service.verifyRegistration("dev-only-token"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("enabled human verification fails closed without a token or provider secret")
    void enabledHumanVerificationFailsClosed() {
        CustomerAuthProperties properties = new CustomerAuthProperties();
        properties.getCaptcha().setEnabled(true);
        HumanVerificationService service = new HumanVerificationService(properties, new OkHttpClient(), new ObjectMapper());

        // Scenario: production registration must not silently pass when clients omit the human challenge.
        assertThatThrownBy(() -> service.verifyRegistration(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("human verification is required");
        assertThatThrownBy(() -> service.verifyRegistration("provider-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("human verification secret is not configured");
    }
}
