/*
 * 檔案用途：基礎設施設定，提供資金費批次結算參數。
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
@ConfigurationProperties(prefix = "funding-rate")
public class FundingRateProperties {

    /**
     * 批次資金費結算設定；mark price 由 MarkPriceOracleService 提供。
     */
    private List<Settlement> settlements = new ArrayList<>();

    @Data
    public static class Settlement {
        private String symbol;
        /**
         * 舊設定欄位保留給設定相容性；結算路徑不再使用任意輸入 mark price。
         */
        @Deprecated
        private BigDecimal markPrice;
        private BigDecimal fundingRate;
    }
}
