/*
 * File purpose: Verify protected API path classification.
 */
package com.example.exchange.interfaces.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtectedApiClassifierTest {

    @Test
    @DisplayName("admin API paths are classified as ADMIN")
    /**
     * Scenario: admin market-config endpoint is protected as an admin operation.
     */
    void classifiesAdminApiPathAsAdmin() {
        assertThat(ProtectedApiClassifier.classify("/api/admin/market-config"))
                .isEqualTo(ProtectedApiCategory.ADMIN);
    }
}
