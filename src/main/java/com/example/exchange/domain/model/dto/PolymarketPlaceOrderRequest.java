package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.PolymarketOrderDirection;
import com.example.exchange.domain.model.enums.PolymarketOrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Polymarket place order request。
 *
 * 這是前端呼叫：
 *
 * POST /api/prediction/orders
 *
 * 使用的下單 request。
 *
 * 正式流程：
 * 1. 前端先完成 /session/init
 * 2. 前端完成 /session/confirm
 * 3. 前端拿 ACTIVE sessionId
 * 4. 下單時帶 sessionId
 * 5. 後端使用 session signer private key 簽 CLOB order
 *
 * 注意：
 * 這裡傳入的是你平台整理後的 market/outcome 資料，
 * 不是直接傳 Polymarket 原始 response。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolymarketPlaceOrderRequest {

    /**
     * 你平台內部 user id。
     *
     * TODO:
     * 正式環境應從登入態 / JWT / session 中取得，
     * 不應完全信任前端傳入。
     */
    @NotBlank
    private String userId;

    /**
     * 事件 slug。
     *
     * 例如：
     * fifwc-prt-uzb-2026-06-23
     */
    @NotBlank
    private String eventSlug;

    /**
     * Polymarket market slug。
     *
     * 例如：
     * fifwc-prt-uzb-2026-06-23-prt
     */
    @NotBlank
    private String marketSlug;

    /**
     * outcome key。
     *
     * 例如：
     * homeWin / draw / awayWin
     */
    @NotBlank
    private String outcomeKey;

    /**
     * ACTIVE session id。
     *
     * placeOrder 時：
     * 1. 查 ACTIVE session
     * 2. 取得 session signer private key
     * 3. 使用 session signer 簽 CLOB order
     *
     * TODO:
     * 後端還需要確認：
     * session.userAddress 是否對應當前登入使用者。
     */
    @NotBlank
    private String sessionId;

    /**
     * YES token id。
     *
     * BUY_YES / SELL_YES 使用。
     */
    @NotBlank
    private String yesTokenId;

    /**
     * NO token id。
     *
     * BUY_NO / SELL_NO 使用。
     */
    @NotBlank
    private String noTokenId;

    /**
     * BUY YES 價格。
     *
     * 來源通常是：
     * ask / bestAsk。
     */
    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal yesBuyPrice;

    /**
     * SELL YES 價格。
     *
     * 來源通常是：
     * bid / bestBid。
     */
    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal yesSellPrice;

    /**
     * BUY NO 價格。
     *
     * Binary market 常見推導：
     * noBuyPrice = 1 - yesSellPrice
     */
    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal noBuyPrice;

    /**
     * SELL NO 價格。
     *
     * Binary market 常見推導：
     * noSellPrice = 1 - yesBuyPrice
     */
    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal noSellPrice;

    /**
     * 使用者交易方向。
     *
     * 支援：
     * - BUY_YES
     * - SELL_YES
     * - BUY_NO
     * - SELL_NO
     */
    @NotNull
    private PolymarketOrderDirection direction;

    /**
     * 使用者輸入的 USDT / USDC 金額。
     *
     * 目前你的產品邏輯是：
     * 使用者輸入金額，不直接輸入 shares。
     *
     * 後端會依 price 換算：
     * size = usdtAmount / price
     */
    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal usdtAmount;

    /**
     * Polymarket order type。
     *
     * 建議：
     * - POC / 市價立即成交：FOK
     * - 一般掛單：GTC
     *
     * 若前端不傳，Mapper / Service 可預設 FOK。
     */
    private PolymarketOrderType orderType;

    /**
     * 是否為 Neg Risk market。
     *
     * FIFA / Sports / multi-outcome market 通常為 true。
     *
     * TODO:
     * 正式環境最好不要信任前端傳入。
     * 應該由後端根據 marketSlug / conditionId / market info 查 DB 判斷。
     */
    private Boolean negRisk;
}