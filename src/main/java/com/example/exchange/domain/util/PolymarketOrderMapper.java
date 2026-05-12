package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PolymarketNormalizedClobOrder;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderRequest;
import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.domain.model.enums.PolymarketOrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PolymarketOrderMapper {

    private PolymarketOrderMapper() {
    }

    public static PolymarketNormalizedClobOrder toClobOrder(
            PolymarketPlaceOrderRequest request
    ) {
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
                 * FIFA / sports 這種多 outcome / neg-risk market
                 * 下單簽名時要用 Neg Risk Exchange。
                 *
                 * 如果你的 request 已經有 negRisk 欄位，
                 * 這裡改成：
                 *
                 * .negRisk(Boolean.TRUE.equals(request.getNegRisk()))
                 */
                .negRisk(true)
                .build();
    }
}