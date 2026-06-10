/*
 * File purpose: Local exchange logout request DTO.
 */
package com.example.exchange.interfaces.web.dto;

public record LogoutRequest(
        // Refresh token is the server-side revocation handle; access JWTs expire naturally.
        String refreshToken
) {
}
