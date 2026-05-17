/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Session signer 初始化 response。
 *
 * 前端流程：
 *
 * 1. MetaMask connect
 * 2. POST /api/prediction/session/init
 * 3. 取得 typedData
 * 4. eth_signTypedData_v4
 * 5. POST /api/prediction/session/confirm
 *
 * 注意：
 * 這裡不使用 personal_sign。
 * 這裡走正式 EIP-712 typedData 流程。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitResponse {

    /**
     * Session UUID。
     *
     * 後續 confirm session、
     * place order 都會用到。
     */
    private String sessionId;

    /**
     * 使用者 MetaMask address。
     */
    private String userAddress;

    /**
     * 後端生成的 session signer address。
     *
     * 後續：
     * - place order
     * - cancel order
     *
     * 都會使用這把 signer。
     */
    private String sessionSignerAddress;

    /**
     * EIP-712 typedData。
     *
     * 前端必須使用：
     *
     * window.ethereum.request({
     *   method: "eth_signTypedData_v4",
     *   params: [
     *      userAddress,
     *      JSON.stringify(typedData)
     *   ]
     * })
     */
    private Map<String, Object> typedData;

    /**
     * Session 狀態。
     *
     * 初始：
     * PENDING
     *
     * confirm 後：
     * ACTIVE
     */
    private String status;

    /**
     * Session issue timestamp。
     *
     * Unix epoch seconds。
     */
    private Long issuedAt;

    /**
     * Session expire timestamp。
     *
     * Unix epoch seconds。
     */
    private Long expiresAt;
}