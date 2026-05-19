/*
 * 檔案用途：基礎設施設定，提供 reconciliation report 排程與 alert policy。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "reconciliation")
public class ReconciliationProperties {

    /**
     * 是否啟用 scheduled reconciliation。需搭配 Spring scheduling 啟用。
     */
    private boolean enabled = false;

    /**
     * 排程間隔，預設每 5 分鐘。
     */
    private long fixedDelayMs = 300_000L;

    /**
     * true 時，報告中有 ERROR issue 會輸出 alert log。
     */
    private boolean alertOnError = true;

    /**
     * alert routing baseline。production 可映射到 PagerDuty / Slack / OpsGenie。
     */
    private String alertRoute = "ops.reconciliation";
}
