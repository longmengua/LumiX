/*
 * 檔案用途：archive exporter 排程與資料族設定。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "archive.exporter")
public class ArchiveExporterProperties {

    /** 預設關閉，避免未配置 object storage / reviewer 流程時自動執行。 */
    private boolean enabled = false;

    /** Scheduler fixed delay；實際排程仍受 Spring scheduling 是否啟用影響。 */
    private long fixedDelayMs = 86_400_000L;

    /** 預設匯出前一個 UTC 日。 */
    private long daysBack = 1L;

    private boolean ordersEnabled = true;
    private boolean tradesEnabled = true;
    private boolean ledgerEnabled = true;
}
