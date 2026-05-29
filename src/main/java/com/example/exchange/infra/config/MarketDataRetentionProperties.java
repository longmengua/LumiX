/*
 * 檔案用途：基礎設施設定，控制 market-data history retention。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "market-data.retention")
public class MarketDataRetentionProperties {

    /** 是否啟用 market-data retention 排程；預設關閉，避免本機或測試環境自動刪資料。 */
    private boolean enabled = false;

    /** retention job fixed delay；實際啟用仍受 ExchangeApplication 的 scheduling 開關控制。 */
    private long fixedDelayMs = 300_000L;

    /** Depth delta backfill history retention。 */
    private Duration depthDelta = Duration.ofHours(24);

    /** Trade tape history retention。 */
    private Duration tradeTape = Duration.ofDays(7);

    /** 1m kline history retention。 */
    private Duration kline = Duration.ofDays(30);
}
