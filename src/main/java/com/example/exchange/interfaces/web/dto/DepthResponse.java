/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 深度查詢回應 DTO
 */
public record DepthResponse(
        String symbol,
        BigDecimal bestBid,
        BigDecimal bestAsk,
        List<Level> bids,   // 由高到低
        List<Level> asks    // 由低到高
) {
    /** 價量 */
    public record Level(BigDecimal price, BigDecimal qty) {}
}
