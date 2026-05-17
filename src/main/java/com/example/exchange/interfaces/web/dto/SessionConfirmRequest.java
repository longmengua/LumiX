/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionConfirmRequest {

    private String sessionId;

    private String userAddress;

    /**
     * MetaMask personal_sign 回傳的 signature。
     */
    private String signature;
}
