/*
 * 檔案用途：基礎設施設定，控制內建做市商自動報價策略與背景 runner。
 */
package com.example.exchange.infra.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Data
@Validated
@ConfigurationProperties(prefix = "market-maker.auto-quote")
public class MarketMakerAutoQuoteProperties {

    /**
     * 預設關閉，避免本機或 production 啟動時自動替所有 enabled 做市商改單。
     */
    private boolean enabled = false;

    /**
     * 自動報價固定間隔；demo 可設 300ms 觀察 WebSocket/order book 動態刷新。
     */
    @Min(100)
    private long fixedDelayMs = 300;

    /**
     * 單次 loop 最多處理多少個 profile，避免大量 profile 時一輪佔用太久。
     */
    @Min(1)
    private int maxProfilesPerRun = 20;

    /**
     * 每邊基礎掛單量；實際量會依 symbol lotSize、minNotional 與 risk limit 修正。
     */
    @Positive
    private BigDecimal quoteQuantity = new BigDecimal("0.100");

    /**
     * 中點上下各保留幾個 tick；至少 1 tick，避免 bid/ask 交叉。
     */
    @Min(1)
    private int halfSpreadTicks = 2;

    /**
     * 外部 venue 對沖額外成本 buffer；安全做市先覆蓋 maker fee、hedge taker fee 與此 buffer。
     */
    @PositiveOrZero
    private BigDecimal hedgeCostRate = new BigDecimal("0.0005");

    /**
     * 每邊 ladder 掛單層數；預設買賣各 50 ticks，前台可只顯示較少層數。
     */
    @Min(1)
    private int ladderLevelsPerSide = 50;

    /**
     * 讓 quote 在中點附近做小幅 tick pulse，demo 時可看見持續 replacement。
     */
    @Min(0)
    private int pulseTicks = 1;

    /**
     * refId prefix 用來辨識自動做市產生的 quote command。
     */
    @NotBlank
    private String refPrefix = "auto-mm";
}
