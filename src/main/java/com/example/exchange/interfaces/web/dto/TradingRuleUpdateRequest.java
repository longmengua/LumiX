/*
 * File purpose: Admin request DTO for changing symbol trading-rule inputs used by pre-trade validation.
 */
package com.example.exchange.interfaces.web.dto;

import java.math.BigDecimal;

public record TradingRuleUpdateRequest(
        BigDecimal priceTick,
        BigDecimal lotSize,
        BigDecimal minQty,
        BigDecimal minNotional,
        BigDecimal maxOrderNotional,
        Integer maxOpenOrders,
        BigDecimal priceBandRate
) {
}
