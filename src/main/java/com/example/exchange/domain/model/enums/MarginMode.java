/*
 * 檔案用途：領域列舉，限制訂單、交易方向、保證金與 Polymarket 狀態的可用值。
 */
package com.example.exchange.domain.model.enums;

/**
 * Margin 模式
 *
 * CROSS: 共用資金池
 * ISOLATED: 每個 symbol 獨立 margin
 */
public enum MarginMode {
    CROSS,
    ISOLATED
}
