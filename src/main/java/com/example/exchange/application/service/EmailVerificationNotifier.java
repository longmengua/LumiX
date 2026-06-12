/*
 * File purpose: Dispatch email-verification links without exposing tokens in normal API responses.
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.CustomerAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationNotifier {

    private final CustomerAuthProperties properties;

    /** Sends or logs the verification URL; SMTP/provider wiring can replace this without changing AuthService. */
    public void sendVerification(String email, String verificationUrl) {
        if (!properties.getEmailVerification().isEnabled()) {
            return;
        }
        // Production should route this through a mail provider; logging keeps local MVP verification usable.
        log.info("Email verification required for {}. verificationUrl={}", email, verificationUrl);
    }
}
