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

        // TODO(HUMAN_REVIEW_REQUIRED): 目前直接回傳 index price 作為占位，避免誤以為已完成正式 mark-price 公式。
        return new MarkPriceView(
                symbol.trim(),
                priceIndexView.indexPrice(),
                priceIndexView.indexPrice(),
                Instant.now()
        );
    }
}
