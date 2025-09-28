package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.OrderSide;
import com.example.exchange.domain.model.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OrderInfoResponse
 * --------------------------
 * API 專用的訂單回應 DTO
 * - 專門用於 REST API 對外回傳訂單資料
 * - 將領域模型 Order 轉換為此 DTO，避免直接暴露內部實作
 */
public record OrderInfoResponse(

        /** 訂單 ID
         * - 唯一識別碼 (通常為 UUID 或資料庫主鍵)
         * - 轉為字串型別，方便前端處理
         */
        String orderId,

        /** 使用者 ID
         * - 下此訂單的用戶
         */
        Long uid,

        /** 交易對 (例如 "BTCUSDT")
         * - 代表此訂單所屬的交易市場
         */
        String symbol,

        /** 下單方向
         * - BUY = 買單
         * - SELL = 賣單
         */
        OrderSide side,

        /** 訂單類型
         * - LIMIT  = 限價單
         * - MARKET = 市價單
         */
        OrderType type,

        /** 價格
         * - 限價單：由用戶指定
         * - 市價單：系統可能自動帶入虛擬價格
         */
        BigDecimal price,

        /** 數量
         * - 訂單下單的數量
         */
        BigDecimal qty,

        /** 訂單狀態
         * - 例如：OPEN, PARTIALLY_FILLED, FILLED, CANCELED
         * - 使用字串而非 enum，是為了前端顯示與相容性
         */
        String status,

        /** 建立時間
         * - 訂單產生的時間
         * - 使用 Instant（UTC 時間戳）
         */
        Instant ctime
) {}
