package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitRequest {

    /**
     * 前端 MetaMask 連線後取得的地址。
     */
    private String userAddress;

    /**
     * 單筆最大下單 USDC。
     */
    private BigDecimal maxOrderUsdt;

    /**
     * 每日最大下單 USDC。
     */
    private BigDecimal dailyLimitUsdt;
}
