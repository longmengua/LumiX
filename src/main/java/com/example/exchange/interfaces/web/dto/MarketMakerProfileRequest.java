/*
 * 檔案用途：REST request DTO，承載做市商 profile 與 risk limits 設定。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MarketMakerProfileRequest(
        @NotBlank String marketMakerId,
        @Positive long uid,
        boolean enabled,
        List<@Valid MarketMakerRiskLimitRequest> riskLimits
) {
    public MarketMakerProfile toProfile() {
        return new MarketMakerProfile(
                marketMakerId == null ? null : marketMakerId.trim(),
                uid,
                enabled,
                riskLimits == null
                        ? List.of()
                        : riskLimits.stream().map(MarketMakerRiskLimitRequest::toLimit).toList()
        );
    }
}
