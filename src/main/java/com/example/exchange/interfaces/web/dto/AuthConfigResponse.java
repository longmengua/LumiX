/*
 * File purpose: Public auth configuration needed by the static customer login form.
 */
package com.example.exchange.interfaces.web.dto;

public record AuthConfigResponse(
        boolean humanVerificationEnabled,
        String humanVerificationProvider,
        String humanVerificationSiteKey,
        boolean emailVerificationEnabled
) {
}
