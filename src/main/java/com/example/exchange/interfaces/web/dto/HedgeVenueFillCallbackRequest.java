/*
 * 檔案用途：REST request DTO，承接外部 hedge venue fill callback。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.dto.HedgeVenueFillMessage;
import com.example.exchange.domain.model.enums.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record HedgeVenueFillCallbackRequest(
        @NotBlank String marketMakerId,
        @NotBlank String symbol,
        @NotBlank String venueOrderId,
        @NotBlank String venueFillId,
        @NotNull OrderSide side,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        BigDecimal fee,
        String feeAsset,
        String refId,
        Instant filledAt
) {
    public HedgeVenueFillMessage toMessage() {
        return new HedgeVenueFillMessage(
                marketMakerId,
                symbol,
                venueOrderId,
                venueFillId,
                side,
                quantity,
                price,
                fee,
                feeAsset,
                refId,
                filledAt
        );
    }
}
