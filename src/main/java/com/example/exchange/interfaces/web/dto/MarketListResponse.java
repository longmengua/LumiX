/*
 * File purpose: Public market list response for client trading surfaces.
 */
package com.example.exchange.interfaces.web.dto;

import java.util.List;
import java.math.BigDecimal;

public record MarketListResponse(
        List<MarketItem> markets
) {

    public record MarketItem(
            String symbol,
            String productType,
            String baseAsset,
            String quoteAsset,
            boolean tradingEnabled,
            BigDecimal priceTick,
            BigDecimal lotSize,
            BigDecimal minQty,
            BigDecimal minNotional
    ) {
    }
}
