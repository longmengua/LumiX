package com.example.exchange.domain.model.enums;

/**
 * Polymarket / CLOB order type。
 *
 * GTC:
 * Good Till Cancel。
 *
 * 掛單持續存在，
 * 直到：
 * 1. 成交
 * 2. 使用者取消
 * 3. 過期
 *
 * 適合：
 * maker / 掛單。
 *
 * --------------------------------------------------
 *
 * FOK:
 * Fill Or Kill。
 *
 * 必須：
 * 一次全部成交。
 *
 * 否則：
 * 直接取消。
 *
 * 不允許部分成交。
 *
 * 適合：
 * 市價立即成交。
 *
 * --------------------------------------------------
 *
 * FAK:
 * Fill And Kill。
 *
 * 能成交多少就成交多少，
 * 剩餘部分取消。
 *
 * 允許部分成交。
 *
 * 適合：
 * 吃部分流動性。
 *
 * --------------------------------------------------
 *
 * GTD:
 * Good Till Date。
 *
 * 掛單有效到指定時間。
 *
 * 到期後：
 * 自動取消。
 *
 * TODO:
 * placeOrder 時需額外帶：
 * expiration timestamp。
 */
public enum PolymarketOrderType {

    /**
     * Good Till Cancel。
     */
    GTC,

    /**
     * Fill Or Kill。
     */
    FOK,

    /**
     * Fill And Kill。
     */
    FAK,

    /**
     * Good Till Date。
     */
    GTD
}