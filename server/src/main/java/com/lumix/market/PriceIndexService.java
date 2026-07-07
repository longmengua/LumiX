package com.lumix.market;

import java.util.List;

/**
 * 指數價格服務契約。
 */
public interface PriceIndexService {

    // TODO(HUMAN_REVIEW_REQUIRED): 取得外部價格指數；正式版本必須定義來源、加權與異常值過濾規則。
    List<ExternalPriceQuote> listExternalQuotes(String symbol);

    // 回傳用於索引計算的來源清單，便於驗證與維運。
    PriceIndexView getPriceIndex(String symbol);
}
