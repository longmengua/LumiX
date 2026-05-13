package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitRequest {

    /**
     * 前端 MetaMask 連線後取得的地址。
     */
    private String userAddress;
}
