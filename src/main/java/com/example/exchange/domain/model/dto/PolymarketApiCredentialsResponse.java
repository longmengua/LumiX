package com.example.exchange.domain.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Polymarket CLOB API credentials.
 */
@Data
@Builder
public class PolymarketApiCredentialsResponse {

    private boolean success;

    private String apiKey;

    private String secret;

    private String passphrase;

    private String signerAddress;

    private String nonce;

    private String errorMsg;
}
