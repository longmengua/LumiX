/*
 * 檔案用途：基礎設施設定，提供 production matching worker ownership 與 lease 參數。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "matching-worker")
public class MatchingWorkerProperties {

    /**
     * 是否啟用獨立 matching worker command intake；預設關閉，避免 REST app 未切流前誤啟。
     */
    private boolean enabled = false;

    /**
     * Worker owner id。Production 應使用 pod / instance / process 唯一值。
     */
    private String ownerId = "local-dev";

    /**
     * 此 worker 負責取得 lease 的 symbol 清單。
     */
    private List<String> symbols = new ArrayList<>();

    /**
     * Sequencer lease TTL。必須長於正常 command processing interval，短於 failover 目標。
     */
    private long leaseTtlMs = 30_000L;

    /**
     * Lease renew interval。必須小於 leaseTtlMs。
     */
    private long renewIntervalMs = 10_000L;

    /**
     * true 時，已配置給 worker 的 symbol 若尚未 ready，REST 舊路徑不可 fallback 到 in-process matching。
     */
    private boolean fenceLegacyRouting = false;
}
