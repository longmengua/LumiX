package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Session list response。
 *
 * 用於：
 * 1. session 管理頁
 * 2. revoke session
 * 3. 顯示 ACTIVE / EXPIRED / REVOKED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponse {

    /**
     * Session UUID。
     */
    private String sessionId;

    /**
     * Deposit wallet。
     */
    private String userAddress;

    /**
     * Session signer address。
     */
    private String sessionSignerAddress;

    /**
     * PENDING / ACTIVE / EXPIRED / REVOKED
     */
    private String status;

    /**
     * Unix epoch seconds。
     */
    private Long issuedAt;

    /**
     * Unix epoch seconds。
     */
    private Long expiresAt;

    /**
     * ISO timestamp。
     */
    private String createdAt;

    /**
     * Session confirm time。
     */
    private String confirmedAt;

    /**
     * 最後使用時間。
     */
    private String lastUsedAt;

    private BigDecimal maxOrderUsdt;

    private BigDecimal dailyLimitUsdt;

    private BigDecimal dailyUsedUsdt;

    private String dailyResetDate;
}
