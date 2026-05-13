package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PolymarketNormalizedClobOrder;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderRequest;
import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.domain.model.enums.PolymarketOrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Polymarket order mapper。
 *
 * 職責：
 * 將前端下單 request 轉成後端內部標準化 CLOB order。
 *
 * 前端語意：
 * - BUY_YES
 * - SELL_YES
 * - BUY_NO
 * - SELL_NO
 *
 * CLOB 實際語意：
 * - tokenId
 * - BUY / SELL
 * - price
 * - size
 *
 * 注意：
 * 這裡只做 mapping，不做簽名、不送 CLOB、不查 DB。
 */
public class PolymarketOrderMapper {

    private PolymarketOrderMapper() {
    }

    public static PolymarketNormalizedClobOrder toClobOrder(
            PolymarketPlaceOrderRequest request
    ) {
        validate(request);

        PolymarketOrderType orderType = request.getOrderType() == null
                ? PolymarketOrderType.FOK
                : request.getOrderType();

        String tokenId;
        PolymarketClobSide side;
        BigDecimal price;

        switch (request.getDirection()) {
            case BUY_YES -> {
                tokenId = request.getYesTokenId();
                side = PolymarketClobSide.BUY;
                price = request.getYesBuyPrice();
            }
            case SELL_YES -> {
                tokenId = request.getYesTokenId();
                side = PolymarketClobSide.SELL;
                price = request.getYesSellPrice();
            }
            case BUY_NO -> {
                tokenId = request.getNoTokenId();
                side = PolymarketClobSide.BUY;
                price = request.getNoBuyPrice();
            }
            case SELL_NO -> {
                tokenId = request.getNoTokenId();
                side = PolymarketClobSide.SELL;
                price = request.getNoSellPrice();
            }
            default -> throw new IllegalArgumentException("unsupported direction");
        }

        /**
         * 使用者只輸入 USDT / USDC 金額。
         *
         * 後端換算 shares：
         * size = usdtAmount / price
         *
         * RoundingMode.DOWN：
         * 避免超花使用者資金。
         *
         * TODO:
         * 正式環境建議依 Polymarket tickSize / minSize
         * 做額外 rounding / validation。
         */
        BigDecimal size = request.getUsdtAmount()
                .divide(price, 6, RoundingMode.DOWN);

        return PolymarketNormalizedClobOrder.builder()
                .userId(request.getUserId())
                .eventSlug(request.getEventSlug())
                .marketSlug(request.getMarketSlug())
                .outcomeKey(request.getOutcomeKey())
                .tokenId(tokenId)
                .side(side)
                .price(price)
                .size(size)
                .usdtAmount(request.getUsdtAmount())
                .orderType(orderType)

                /**
                 * 是否使用 NegRisk Exchange。
                 *
                 * FIFA / Sports / multi-outcome 通常為 true。
                 *
                 * 注意：
                 * 不要長期信任前端傳入。
                 *
                 * TODO:
                 * 正式環境建議：
                 * 1. 根據 marketSlug 查 DB
                 * 2. 由後端 market info 決定 negRisk
                 * 3. 不由前端決定
                 */
                .negRisk(Boolean.TRUE.equals(request.getNegRisk()))
                .build();
    }

    /**
     * Mapper 層基礎防呆。
     *
     * Service 層已經會做 validation，
     * 這裡再做一次是避免其他內部程式直接呼叫 mapper 時出錯。
     */
    private static void validate(
            PolymarketPlaceOrderRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        if (request.getDirection() == null) {
            throw new IllegalArgumentException("direction is required");
        }

        if (request.getUsdtAmount() == null || request.getUsdtAmount().signum() <= 0) {
            throw new IllegalArgumentException("usdtAmount must be positive");
        }
    }
}