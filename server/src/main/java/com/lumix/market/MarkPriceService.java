package com.lumix.market;

/**
 * 標記價格服務契約。
 */
public interface MarkPriceService {

    // TODO: requires high-reasoning review before production use
    MarkPriceView getMarkPrice(String symbol);
}
