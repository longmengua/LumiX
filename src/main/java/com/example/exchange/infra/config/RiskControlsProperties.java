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
     * 停牌 symbol 清單，例如 BTCUSDT、ETHUSDT。
     */
    private List<String> suspendedSymbols = new ArrayList<>();
}
