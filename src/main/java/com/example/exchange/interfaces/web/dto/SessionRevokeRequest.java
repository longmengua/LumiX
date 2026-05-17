/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Revoke session request。
 *
 * 使用者可：
 * 1. revoke 單一 session
 * 2. 停止某裝置交易權限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionRevokeRequest {

    /**
     * Session UUID。
     */
    @NotBlank
    private String sessionId;

    /**
     * Deposit wallet。
     *
     * 用於 verify owner。
     */
    @NotBlank
    private String userAddress;
}