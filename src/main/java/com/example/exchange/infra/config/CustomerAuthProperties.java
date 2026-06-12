/*
 * File purpose: Customer-facing auth hardening settings for email verification and free human verification.
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "customer-auth")
public class CustomerAuthProperties {

    /** Email verification blocks login until the user proves mailbox ownership. */
    private EmailVerification emailVerification = new EmailVerification();

    /** Human verification protects public registration from scripted abuse. */
    private Captcha captcha = new Captcha();

    @Data
    public static class EmailVerification {
        private boolean enabled = false;
        private int tokenTtlMinutes = 30;
        private int registrationTtlHours = 24;
        private String publicBaseUrl = "http://127.0.0.1:8080/exchange.html";
        private boolean returnVerificationUrl = false;
        /** Optional first-party SMTP delivery; when disabled, local/dev falls back to logging the URL. */
        private Smtp smtp = new Smtp();
    }

    @Data
    public static class Smtp {
        private boolean enabled = false;
        private String host = "";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "";
        private String subject = "Verify your exchange account";
        private boolean auth = true;
        private boolean startTls = true;
        private boolean ssl = false;
        private int timeoutMs = 5000;
    }

    @Data
    public static class Captcha {
        private boolean enabled = false;
        private String provider = "turnstile";
        private String siteKey = "";
        private String secret = "";
        private String verifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        private String devBypassToken = "";
    }
}
