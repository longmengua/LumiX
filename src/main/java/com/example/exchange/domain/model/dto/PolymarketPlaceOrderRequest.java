package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.PolymarketOrderDirection;
import com.example.exchange.domain.model.enums.PolymarketOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolymarketPlaceOrderRequest {

    private String userId;

    private String eventSlug;

    private String marketSlug;

    private String outcomeKey;

    /**
     * YES token
     */
    private String yesTokenId;

    /**
     * NO token
     */
    private String noTokenId;

    /**
     * BUY YES
     */
    private BigDecimal yesBuyPrice;

    /**
     * SELL YES
     */
    private BigDecimal yesSellPrice;

    /**
     * BUY NO
     */
    private BigDecimal noBuyPrice;

    /**
     * SELL NO
     */
    private BigDecimal noSellPrice;

    /**
     * BUY_YES / SELL_YES / BUY_NO / SELL_NO
     */
    private PolymarketOrderDirection direction;

    /**
     * 使用者輸入 USDT
     */
    private BigDecimal usdtAmount;

    /**
     * 預設 FOK
     */
    private PolymarketOrderType orderType;
}