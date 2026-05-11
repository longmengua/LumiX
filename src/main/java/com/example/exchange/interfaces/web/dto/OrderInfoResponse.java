package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OrderInfoResponse
 * -------------------------------------------------
 * API 專用的訂單回應 DTO
 *
 * 用途：
 * - 專門用於 REST API 對外回傳訂單資料
 * - 避免直接把 Domain Model（Order）暴露給前端
 * - 讓回應格式穩定，降低內部模型調整對 API 的影響
 *
 * 設計說明：
 * - 本 DTO 主要提供前端或 API 呼叫端查詢訂單資訊使用
 * - 包含訂單基本資料、數量狀態、成交資訊、有效期、拒單原因等欄位
 * - 狀態與 TimeInForce 使用字串輸出，方便前端顯示與跨語言相容
 *
 * 常見使用情境：
 * - 查詢當前掛單（open orders）
 * - 查詢歷史訂單（all orders）
 * - 下單後回查訂單狀態
 */
public record OrderInfoResponse(

        /**
         * 訂單 ID
         * - 系統內部唯一識別碼
         * - 常見為 UUID、雪花 ID 或資料庫主鍵
         * - 轉為字串，方便前端與 JSON 傳輸處理
         */
        String orderId,

        /**
         * 使用者 ID
         * - 下這張訂單的使用者
         */
        Long uid,

        /**
         * 交易對代碼
         * - 例如：BTCUSDT
         * - 表示這張訂單所屬的交易市場
         */
        String symbol,

        /**
         * 下單方向
         * - BUY  = 買單
         * - SELL = 賣單
         */
        OrderSide side,

        /**
         * 訂單類型
         * - LIMIT  = 限價單
         * - MARKET = 市價單
         */
        OrderType type,

        /**
         * 委託價格
         * - LIMIT：通常為使用者指定價格
         * - MARKET：本專案可能以極端價格模擬撮合
         */
        BigDecimal price,

        /**
         * 原始委託數量
         * - 使用者最初提交的下單量
         * - 不會因成交而改變
         */
        BigDecimal origQty,

        /**
         * 剩餘未成交數量
         * - 會隨著撮合成交而遞減
         * - 若為 0，通常代表已全部成交或已無剩餘量
         */
        BigDecimal qty,

        /**
         * 已成交數量
         * - 累積已成交的數量
         * - 理論上通常滿足：origQty = executedQty + qty
         */
        BigDecimal executedQty,

        /**
         * 平均成交價
         * - 若已有部分或全部成交，表示加權平均成交價格
         * - 若尚未成交，通常為 0 或 null（依系統約定）
         */
        BigDecimal avgPrice,

        /**
         * 訂單有效期（Time In Force）
         * - GTC：未成交部分持續掛單直到取消
         * - IOC：立即成交可成交部分，剩餘失效
         * - FOK：必須一次全成，否則整張失效
         *
         * 使用字串而非 enum：
         * - 方便前端顯示
         * - 降低跨語言序列化耦合
         */
        String timeInForce,

        /**
         * 是否為 reduce-only 訂單
         * - true  = 只能減倉
         * - false = 一般訂單
         */
        boolean reduceOnly,

        /**
         * 客戶端自訂訂單 ID
         * - 通常由前端、策略程式、量化系統自行傳入
         * - 可用於冪等控制、追蹤與對帳
         */
        String clientOrderId,

        /**
         * 訂單狀態
         * - 例如：NEW、PARTIALLY_FILLED、FILLED、CANCELED、REJECTED、EXPIRED
         *
         * 使用字串而非 enum：
         * - 前端顯示更直接
         * - 與外部系統整合更穩定
         */
        String status,

        /**
         * 拒單原因碼
         * - 僅在 REJECTED 狀態時通常有值
         * - 例如：MARGIN_INSUFF、INVALID_PRICE、REDUCE_ONLY_VIOLATION
         */
        String rejectCode,

        /**
         * 建立時間
         * - 訂單建立的時間點
         * - 使用 Instant，代表 UTC 時間戳
         */
        Instant ctime
) {}