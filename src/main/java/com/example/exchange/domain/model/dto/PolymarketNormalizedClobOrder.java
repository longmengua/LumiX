/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.domain.model.enums.PolymarketOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 標準化後的 Polymarket CLOB order。
 *
 * 用途：
 * 前端 request 經過 mapper 後，
 * 轉成後端內部統一格式。
 *
 * 流程：
 *
 * PolymarketPlaceOrderRequest
 * -> PolymarketOrderMapper
 * -> PolymarketNormalizedClobOrder
 * -> PolymarketOrderSigner
 * -> EIP-712 sign
 * -> POST /order
 *
 * 注意：
 * 這個 object 已經是：
 * 「真正準備送 CLOB 的標準化資料」
 *
 * 所以：
 * - tokenId 已決定
 * - BUY / SELL 已決定
 * - size 已換算完成
 * - price 已決定
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolymarketNormalizedClobOrder {

    /**
     * 你平台內部 user id。
     *
     * TODO:
     * 正式環境建議：
     * 不要只靠前端傳入，
     * 應從 auth context / JWT 取得。
     */
    private String userId;

    /**
     * 事件 slug。
     *
     * 例如：
     * fifwc-prt-uzb-2026-06-23
     */
    private String eventSlug;

    /**
     * market slug。
     *
     * 例如：
     * fifwc-prt-uzb-2026-06-23-prt
     */
    private String marketSlug;

    /**
     * outcome key。
     *
     * 例如：
     * homeWin / draw / awayWin
     */
    private String outcomeKey;

    /**
     * 最終實際下單 tokenId。
     *
     * BUY_YES / SELL_YES：
     * -> yesTokenId
     *
     * BUY_NO / SELL_NO：
     * -> noTokenId
     */
    private String tokenId;

    /**
     * 最終 CLOB side。
     *
     * 只有：
     * - BUY
     * - SELL
     *
     * 不再區分 YES / NO。
     */
    private PolymarketClobSide side;

    /**
     * 最終成交價格。
     *
     * 例如：
     * BUY_YES -> yesBuyPrice
     * SELL_NO -> noSellPrice
     */
    private BigDecimal price;

    /**
     * 最終 shares 數量。
     *
     * 換算：
     * size = usdtAmount / price
     *
     * 例如：
     *
     * usdtAmount = 100
     * price = 0.25
     *
     * size = 400 shares
     */
    private BigDecimal size;

    /**
     * 使用者原始輸入金額。
     *
     * 使用者輸入的是：
     * USDT / USDC
     *
     * 不是 shares。
     */
    private BigDecimal usdtAmount;

    /**
     * Polymarket order type。
     *
     * 常見：
     * - FOK
     * - GTC
     */
    private PolymarketOrderType orderType;

    /**
     * 是否為 Neg Risk market。
     *
     * true：
     * - FIFA
     * - Sports
     * - multi-outcome
     *
     * false：
     * - 一般 binary market
     *
     * 用途：
     * EIP-712 signing 時，
     * 決定使用：
     * - Exchange V2
     * - NegRisk Exchange V2
     *
     * TODO:
     * 正式環境建議：
     * 不要由前端決定。
     * 應由後端 market info / DB 決定。
     */
    private Boolean negRisk;
}