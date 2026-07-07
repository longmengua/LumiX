package com.lumix.market;

/**
 * 標記價格服務契約。
 */
public interface MarkPriceService {

    // TODO(HUMAN_REVIEW_REQUIRED): 取得 mark price；正式公式與資料來源要在後續階段定義。
    MarkPriceView getMarkPrice(String symbol);
}
