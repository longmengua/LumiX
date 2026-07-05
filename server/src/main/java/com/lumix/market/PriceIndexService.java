package com.lumix.market;

import java.util.List;

/**
 * 指數價格服務契約。
 */
public interface PriceIndexService {

    // TODO: requires high-reasoning review before production use
    List<ExternalPriceQuote> listExternalQuotes(String symbol);

    // TODO: requires high-reasoning review before production use
    PriceIndexView getPriceIndex(String symbol);
}
