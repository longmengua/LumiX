/*
 * File purpose: Customer locale preference update request for authenticated profile state.
 */
package com.example.exchange.interfaces.web.dto;

public record PreferredLanguageRequest(
        // Locale key must match one of the static frontend translation bundles; service normalizes unsupported values.
        String preferredLanguage
) {
}
