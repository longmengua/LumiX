/*
 * File purpose: Request a new registration email code for a pending customer registration.
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        // Email identifies the pending registration; it is normalized by the service before lookup.
        @Email @NotBlank String email,
        // Current browser locale controls resend email copy without extending registration lifetime.
        String preferredLanguage
) {
}
