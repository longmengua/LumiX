/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.PolymarketClobSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolymarketPlaceOrderResponse {

    private Boolean success;

    private String internalOrderId;

    private String clobOrderId;

    private String status;

    private String tokenId;

    private PolymarketClobSide side;

    private BigDecimal price;

    private BigDecimal size;

    private BigDecimal usdtAmount;

    private String errorMsg;
}
