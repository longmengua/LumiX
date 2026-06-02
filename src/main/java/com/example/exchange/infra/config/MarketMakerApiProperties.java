/*
 * 檔案用途：基礎設施設定，控制 market-maker API 的 command 級限流策略。
 */
package com.example.exchange.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "market-maker.api")
public class MarketMakerApiProperties {

    @Valid
    private QuoteRateLimit quoteRateLimit = new QuoteRateLimit();

    @Valid
    private HedgeExecutionRateLimit hedgeExecutionRateLimit = new HedgeExecutionRateLimit();

    @Data
    public static class QuoteRateLimit {
        /**
         * 是否啟用做市商 quote command 固定視窗限流。
         */
        private boolean enabled = true;

        /**
         * 每個 client + market-maker + symbol 每分鐘允許提交的 quote command 數。
         */
        @Min(1)
        private int quotesPerMinute = 120;

        /**
         * 最多追蹤多少個 client + market-maker + symbol 組合，避免 in-memory limiter 無限成長。
         */
        @Min(1)
        private int maxTrackedKeys = 10000;

        /**
         * 若部署在可信反向代理後方，可從此 header 取得真實 client IP。
         */
        private String clientIpHeader = "X-Forwarded-For";
    }

    @Data
    public static class HedgeExecutionRateLimit {
        /**
         * 是否啟用做市商 hedge execution command 固定視窗限流。
         */
        private boolean enabled = true;

        /**
         * 每個 client + execution scope 每分鐘允許提交的 hedge execution command 數。
         */
        @Min(1)
        private int executionsPerMinute = 30;

        /**
         * 最多追蹤多少個 client + execution scope 組合，避免 in-memory limiter 無限成長。
         */
        @Min(1)
        private int maxTrackedKeys = 10000;

        /**
         * 若部署在可信反向代理後方，可從此 header 取得真實 client IP。
         */
        private String clientIpHeader = "X-Forwarded-For";
    }
}
