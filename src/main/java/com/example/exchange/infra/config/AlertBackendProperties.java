/*
 * 檔案用途：集中管理 alert backend webhook 設定，預設停用以避免本機或測試誤送外部通知。
 */
package com.example.exchange.infra.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "alerts.backend")
public class AlertBackendProperties {

    private boolean enabled = false;

    private String webhookUrl = "";

    @Min(100)
    private int timeoutMs = 3_000;
}
