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
     * 批次資金費結算設定；每個 symbol 一筆 mark price 與 funding rate。
     */
    private List<Settlement> settlements = new ArrayList<>();

    @Data
    public static class Settlement {
        private String symbol;
        private BigDecimal markPrice;
        private BigDecimal fundingRate;
    }
}
