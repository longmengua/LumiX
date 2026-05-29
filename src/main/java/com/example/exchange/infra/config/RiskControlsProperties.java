/*
 * 檔案用途：基礎設施設定，提供全站與單交易對風控開關。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "risk-controls")
public class RiskControlsProperties {

    /**
     * true 時拒絕所有新下單。
     */
    private boolean orderEntryHalt = false;

    /**
     * true 時只允許 reduce-only 訂單。
     */
    private boolean reduceOnlyMode = false;

    /**
     * true 時出金不扣款、不確認，進入人工覆核狀態。
     */
    private boolean withdrawalHalt = false;

    /**
     * true 時拒絕 liquidation execution，供營運緊急暫停強平。
     */
    private boolean liquidationHalt = false;

    /**
     * true 時 liquidation 只記錄 decision audit，不執行平倉，交由人工覆核。
     */
    private boolean liquidationManualReview = false;

    /**
     * 單次 liquidation scan 最多處理幾筆 open positions；0 或負數代表不限制。
     */
    private int liquidationScanBatchSize = 0;

    /**
     * true 時拒絕 market-maker hedge execution，供營運緊急暫停對外對沖。
     */
    private boolean marketMakerHedgeExecutionHalt = false;

    /**
     * 停牌 symbol 清單，例如 BTCUSDT、ETHUSDT。
     */
    private List<String> suspendedSymbols = new ArrayList<>();

    /**
     * 下單頻率限制。預設關閉；production 可先用本機固定視窗 baseline，
     * 多實例部署時再替換為 Redis / gateway 共用計數。
     */
    private OrderEntryFrequencyLimit orderEntryFrequencyLimit =
            new OrderEntryFrequencyLimit();

    /**
     * 做市商 hedge execution policy。預設關閉；production 可用來限制單次 worker run
     * 的對外 hedge order 數，避免 scheduler/worker 異常時瞬間放大 venue 風險。
     */
    private MarketMakerHedgeExecutionPolicy marketMakerHedgeExecutionPolicy =
            new MarketMakerHedgeExecutionPolicy();

    @Data
    public static class OrderEntryFrequencyLimit {

        /**
         * true 時啟用 uid + symbol 固定視窗下單頻率限制。
         */
        private boolean enabled = false;

        /**
         * 單一 uid + symbol 在視窗內允許的最大新單數。
         */
        private int maxOrders = 0;

        /**
         * 固定視窗長度，單位秒。
         */
        private long windowSeconds = 60;
    }

    @Data
    public static class MarketMakerHedgeExecutionPolicy {

        /**
         * true 時啟用 hedge execution policy。
         */
        private boolean enabled = false;

        /**
         * 單次 hedge execution run 最多允許 route 的 venue orders；0 或負數代表不限制。
         */
        private int maxRoutedOrdersPerRun = 0;
    }
}
