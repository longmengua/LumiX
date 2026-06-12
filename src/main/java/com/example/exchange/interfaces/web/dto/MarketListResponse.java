/*
 * File purpose: Public market list response for client trading surfaces.
 */
package com.example.exchange.interfaces.web.dto;

import java.util.List;

public record MarketListResponse(
        List<MarketItem> markets
) {

    public record MarketItem(
            String symbol,
            String baseAsset,
            String quoteAsset,
            boolean tradingEnabled
    ) {
    }
}
