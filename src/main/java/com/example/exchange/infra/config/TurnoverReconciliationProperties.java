/*
 * 檔案用途：基礎設施設定，控制 turnover batch 對帳排程與 window。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "turnover.reconciliation")
public class TurnoverReconciliationProperties {

    /**
     * 預設關閉，production 啟用前需確認 trade tape / ledger journal 延遲與告警路由。
     */
    private boolean enabled = false;

    private long fixedDelayMs = 300_000L;

    private long lookbackSeconds = 3_600L;

    private int batchLimit = 1_000;
}
