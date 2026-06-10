/*
 * File purpose: Local exchange login request DTO.
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        // Email/password login is first-party only for MVP; third-party providers are future TODO.
        @Email @NotBlank String email,
        // Raw password is never logged or returned; service compares against PBKDF2 hash.
        @NotBlank String password
) {
}
