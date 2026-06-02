/*
 * 檔案用途：distributed tracing export 與 sampling policy 設定。
 */
package com.example.exchange.infra.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "tracing.export")
public class TracingExportProperties {

    /** 預設關閉，避免未配置 collector 時啟動外部 export。 */
    private boolean enabled = false;

    /** OTLP collector endpoint；啟用時由環境變數或 secret-backed config 提供。 */
    @NotBlank
    private String otlpEndpoint = "http://localhost:4318/v1/traces";

    /** 主要服務名稱，供 tracing backend 聚合查詢。 */
    @NotBlank
    private String serviceName = "java21-match-hub";

    /** 一般 request ratio sampling，0.0 表示只保留 forced-sampled 流量。 */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double sampleRate = 0.10;

    /** error、security audit、settlement、reconciliation 等關鍵流量應固定保留。 */
    private boolean alwaysSampleCriticalFlows = true;

    /** 健康檢查與 metrics 讀取預設不取樣，避免低價值 traces 壓垮後端。 */
    private boolean dropHealthAndMetrics = true;
}
