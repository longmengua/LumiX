/*
 * File purpose: Local exchange auth response DTO.
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.application.service.AuthService;

import java.time.Instant;

public record AuthResponse(
        // Always Bearer so frontend can attach Authorization consistently.
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        User user
) {
    /** Converts service-layer auth result while hiding persistence-only session details. */
    public static AuthResponse from(AuthService.AuthResult result) {
        return new AuthResponse(
                "Bearer",
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt(),
                User.from(result.user())
        );
    }

    public record User(
            // uid matches app_users.id and the exchange Account uid.
            Long uid,
            String email,
            String roles,
            String scopes
    ) {
        public static User from(AuthService.CurrentUser user) {
            return new User(user.uid(), user.email(), user.roles(), user.scopes());
        }
    }
}
