/*
 * File purpose: Local exchange registration request DTO.
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        // Email is the local login identifier; it is normalized before persistence.
        @Email @NotBlank String email,
        // Password is accepted only at registration time and is immediately hashed server-side.
        @NotBlank @Size(min = 8, max = 128) String password,
        // Public registration can require a free human-verification provider token such as Cloudflare Turnstile.
        String humanVerificationToken,
        // Browser locale at registration time localizes verification email and becomes the user's saved preference.
        String preferredLanguage,
        // Browser IANA time zone lets verification emails show expiry in the customer's local time.
        String timeZone
) {
}
