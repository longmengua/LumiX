/*
 * File purpose: Tests for customer email-verification delivery configuration.
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.CustomerAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationNotifierTest {

    @Test
    @DisplayName("disabled SMTP falls back to local logging without blocking registration")
    void disabledSmtpFallsBackToLogging() {
        CustomerAuthProperties properties = new CustomerAuthProperties();
        properties.getEmailVerification().setEnabled(true);
        EmailVerificationNotifier notifier = new EmailVerificationNotifier(properties);

        // Scenario: demos can keep email verification enabled and still expose the link through logs/dev response.
        assertThatCode(() -> notifier.sendVerification(
                        "alice@example.com",
                        "http://127.0.0.1/verify",
                        "123456",
                        Instant.parse("2026-06-13T00:00:00Z")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("enabled SMTP fails fast when required delivery settings are missing")
    void enabledSmtpRequiresHostAndFromAddress() {
        CustomerAuthProperties properties = new CustomerAuthProperties();
        properties.getEmailVerification().setEnabled(true);
        properties.getEmailVerification().getSmtp().setEnabled(true);
        EmailVerificationNotifier notifier = new EmailVerificationNotifier(properties);

        // Scenario: production must not silently create pending accounts when no SMTP relay can deliver the token.
        assertThatThrownBy(() -> notifier.sendVerification(
                        "alice@example.com",
                        "http://127.0.0.1/verify",
                        "123456",
                        Instant.parse("2026-06-13T00:00:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("smtp host is not configured");

        properties.getEmailVerification().getSmtp().setHost("smtp.example.com");
        assertThatThrownBy(() -> notifier.sendVerification(
                        "alice@example.com",
                        "http://127.0.0.1/verify",
                        "123456",
                        Instant.parse("2026-06-13T00:00:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("smtp from is not configured");
    }
}
