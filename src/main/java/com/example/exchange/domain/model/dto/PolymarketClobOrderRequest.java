/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolymarketClobOrderRequest {

    private SignedOrder order;

    private String owner;

    private String orderType;

    private Boolean deferExec;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignedOrder {

        private String salt;

        private String maker;

        private String signer;

        private String tokenId;

        private String makerAmount;

        private String takerAmount;

        /**
         * 0 = BUY, 1 = SELL
         */
        private String side;

        /**
         * POST /order wire body 仍可帶 expiration。
         */
        private String expiration;

        /**
         * CLOB V2 signed order 使用 timestamp。
         */
        private String timestamp;

        /**
         * bytes32
         */
        private String metadata;

        /**
         * bytes32
         */
        private String builder;

        private String signature;

        private Integer signatureType;
    }
}