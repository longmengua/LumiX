package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionConfirmResponse {

    private String sessionId;

    private String userAddress;

    private String sessionSignerAddress;

    private String status;

    private String message;
}