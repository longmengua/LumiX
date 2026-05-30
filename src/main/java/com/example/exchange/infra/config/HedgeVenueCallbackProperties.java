/*
 * 檔案用途：基礎設施設定，控制 hedge venue callback 簽章驗證。
 */
package com.example.exchange.infra.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "market-maker.hedge-callback")
public class HedgeVenueCallbackProperties {

    /**
     * 是否要求 venue fill callback 帶 HMAC 簽章。
     */
    private boolean signatureRequired = false;

    /**
     * HMAC-SHA256 secret。Production 應由環境變數或 secret manager 注入。
     */
    private String signatureSecret = "";

    /**
     * 簽章 timestamp 容忍秒數，避免舊 callback 被重放。
     */
    @Min(1)
    private long timestampToleranceSeconds = 300;
}
