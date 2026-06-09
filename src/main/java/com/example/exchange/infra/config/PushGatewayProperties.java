/*
 * 檔案用途：push gateway 設定，控制 SSE/WebSocket heartbeat 與 client 連線限流。
 */
package com.example.exchange.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "push-gateway")
public class PushGatewayProperties {

    @Valid
    private RuntimeSettings runtime = new RuntimeSettings();

    @Valid
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RuntimeSettings {
        /**
         * 部署角色：MONOLITH 表示與主 app 同跑，GATEWAY 表示獨立 SSE/WebSocket edge role。
         */
        private String role = "MONOLITH";

        /**
         * gateway instance identity，供 readiness、log 與 load balancer drain 查詢。
         */
        private String instanceId = "local";

        /**
         * false 時拒絕新的 SSE/WebSocket stream，既有連線仍可完成 drain。
         */
        private boolean acceptNewStreams = true;

        /**
         * true 時代表 instance 正在 drain，不接受新 stream。
         */
        private boolean draining = false;
    }

    @Data
    public static class RateLimit {
        /**
         * 是否啟用 SSE/WebSocket stream 訂閱限流。
         */
        private boolean enabled = true;

        /**
         * 每個 client + stream 分類每分鐘允許建立的訂閱/握手次數。
         */
        @Min(1)
        private int subscriptionsPerMinute = 60;

        /**
         * 最多追蹤多少個 client + stream 分類組合，避免 in-memory limiter 無限成長。
         */
        @Min(1)
        private int maxTrackedKeys = 10000;

        /**
         * 若部署在可信反向代理後方，可從此 header 取得真實 client IP。
         */
        private String clientIpHeader = "X-Forwarded-For";
    }
}
