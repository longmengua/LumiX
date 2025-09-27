package com.example.java21_OLAP.domain.model;

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
