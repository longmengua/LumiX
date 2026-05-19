/*
 * 檔案用途：基礎設施設定，控制 account risk snapshot 排程。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "risk-snapshots")
public class RiskSnapshotProperties {

    /** 是否啟用排程產生 account risk snapshots。 */
    private boolean enabled = false;

    /** 每日 snapshot cron；實際啟用仍受 ExchangeApplication 的 scheduling 開關控制。 */
    private String cron = "0 0 0 * * *";

    /** cron 使用時區。 */
    private String zone = "UTC";
}
