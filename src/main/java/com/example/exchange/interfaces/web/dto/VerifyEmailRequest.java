/*
 * File purpose: Email verification request DTO.
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        // Raw token from the verification link; only the hash is stored server-side.
        @NotBlank String token
) {
}
