/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
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
 * 前端：
 * POST /api/prediction/orders
 *
 * 正式架構：
 *
 * 前端：
 * - 不應傳 tokenId
 * - 不應傳價格
 * - 不應傳 negRisk
 *
 * 後端：
 * 根據 marketSlug 查 DB：
 *
 * prediction_market_info
 *
 * 取得：
 * - yes_token_id
 * - no_token_id
 * - best_bid
 * - best_ask
 * - neg_risk
 * - outcome info
 *
 * 然後：
 * 組真正 CLOB order。
 *
 * --------------------------------------------------
 *
 * 真正 CLOB 下單核心其實只需要：
 *
 * 1. tokenId
 * 2. side
 * 3. price
 * 4. size
 *
 * 但這些都應由後端計算，
 * 不應信任前端。
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
     * 正式環境應從：
     * JWT / session / auth context 取得，
     * 不應直接信任前端。
     */
    @NotBlank
    private String userId;

    /**
     * ACTIVE session id。
     *
     * placeOrder 時：
     * 1. 查 ACTIVE session
     * 2. 取得 session signer private key
     * 3. 使用 session signer 簽 CLOB order
     *
     * TODO:
     * 後端需確認：
     * session.userAddress
     * 是否對應當前登入使用者。
     */
    @NotBlank
    private String sessionId;

    /**
     * Polymarket market slug。
     *
     * 後端透過 marketSlug：
     *
     * prediction_market_info
     *
     * 查：
     * - token ids
     * - prices
     * - market info
     *
     * 例如：
     * fifwc-bra-mar-2026-06-13-bra
     */
    @NotBlank
    private String marketSlug;

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
     * 使用者輸入 USDC 金額。
     *
     * 目前你的產品邏輯：
     * 使用者輸入金額，
     * 不直接輸入 share size。
     *
     * 後端：
     * size = usdtAmount / price
     */
    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal usdtAmount;

    /**
     * Polymarket order type。
     *
     * 建議：
     *
     * FOK：
     * 立即成交，
     * POC 最常用。
     *
     * GTC：
     * 掛單。
     *
     * TODO:
     * 若前端不傳，
     * Service 可預設：
     * FOK。
     */
    private PolymarketOrderType orderType;
}