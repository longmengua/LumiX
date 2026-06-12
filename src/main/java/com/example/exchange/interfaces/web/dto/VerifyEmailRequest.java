/*
 * File purpose: Email verification request DTO.
 */
package com.example.exchange.interfaces.web.dto;

public record VerifyEmailRequest(
        // Raw token from the email link; only the hash is stored server-side.
        String token,
        // Email plus six-digit code supports the in-page registration verification step.
        String email,
        String code
) {
}
