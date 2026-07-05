package com.lumix.market;

import java.time.Instant;
import java.util.Objects;

/**
 * Phase 10 標記價格 stub。
 * 只依賴指數價格介面回傳 placeholder，不實作任何真實基差或強平公式。
 */
public class DefaultMarkPriceService implements MarkPriceService {

    private final PriceIndexService priceIndexService;

    public DefaultMarkPriceService(PriceIndexService priceIndexService) {
        this.priceIndexService = Objects.requireNonNull(priceIndexService, "priceIndexService must not be null");
    }

    @Override
    public MarkPriceView getMarkPrice(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }

        PriceIndexView priceIndexView = priceIndexService.getPriceIndex(symbol);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub mirrors indexPrice and does not implement a production mark-price formula.
        return new MarkPriceView(
                symbol.trim(),
                priceIndexView.indexPrice(),
                priceIndexView.indexPrice(),
                Instant.now()
        );
    }
}
