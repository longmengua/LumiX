/*
 * File purpose: Response for customer registration before email verification completes.
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.application.service.AuthService;

public record RegistrationResponse(
        Long uid,
        String email,
        boolean emailVerificationRequired,
        String verificationUrl
) {
    public static RegistrationResponse from(AuthService.RegistrationResult result) {
        return new RegistrationResponse(
                result.uid(),
                result.email(),
                result.emailVerificationRequired(),
                result.verificationUrl()
        );
    }
}
