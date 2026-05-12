package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.PolymarketClobSide;
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
public class PolymarketNormalizedClobOrder {
    private String userId;
    private String eventSlug;
    private String marketSlug;
    private String outcomeKey;
    private String tokenId;
    private PolymarketClobSide side;
    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal usdtAmount;
    private PolymarketOrderType orderType;
    private Boolean negRisk;
}
