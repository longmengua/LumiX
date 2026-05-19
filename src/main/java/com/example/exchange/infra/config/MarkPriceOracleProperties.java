/*
 * 檔案用途：基礎設施設定，提供 mark/index price oracle baseline input。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "mark-price-oracle")
public class MarkPriceOracleProperties {

    /**
     * oracle quote 最長有效時間。超過此時間的 mark/index price 視為 stale。
     */
    private long maxStalenessMs = 30_000L;

    /**
     * 啟動時載入的 mark/index price。Production 應由獨立 oracle worker 更新。
     */
    private List<Price> prices = new ArrayList<>();

    @Data
    public static class Price {
        private String symbol;
        private BigDecimal markPrice;
        private BigDecimal indexPrice;
        private String source = "config";
    }
}
