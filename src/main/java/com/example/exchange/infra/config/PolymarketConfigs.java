package com.example.exchange.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Data
@Validated
@ConfigurationProperties(prefix = "polymarket")
public class PolymarketConfigs {

    @Valid
    @NotNull
    private Gamma gamma = new Gamma();

    @Valid
    @NotNull
    private Clob clob = new Clob();

    @Valid
    @NotNull
    private Relayer relayer = new Relayer();

    @Valid
    @NotNull
    private Wallet wallet = new Wallet();

    @Valid
    @NotNull
    private Trading trading = new Trading();

    @Data
    public static class Gamma {
        @NotBlank
        private String baseUrl;
    }

    @Data
    public static class Clob {
        @NotBlank
        private String baseUrl;

        /**
         * Polymarket CLOB API credentials.
         * 不是錢包 private key。
         */
        private String apiKey;
        private String apiSecret;
        private String apiPassphrase;
    }

    @Data
    public static class Relayer {
        @NotBlank
        private String baseUrl;

        private String apiKey;
    }

    @Data
    public static class Wallet {
        /**
         * 平台錢包 private key。
         * 生產環境務必放 KMS / Vault，不要直接放 yml。
         */
        private String privateKey;

        /**
         * Polymarket funder address。
         * 有些簽名/交易場景會用到。
         */
        private String funderAddress;

        /**
         * Polymarket signature type.
         * 一般 EOA 可先用 0。
         */
        private Integer signatureType = 0;
    }

    @Data
    public static class Trading {
        /**
         * 買入加價率，例如 0.10 = 10%
         */
        @NotNull
        @DecimalMin("0")
        private BigDecimal buyMarkupRate = new BigDecimal("0.10");

        /**
         * 賣出獲利手續費，例如 0.005 = 0.5%
         */
        @NotNull
        @DecimalMin("0")
        private BigDecimal sellProfitFeeRate = new BigDecimal("0.005");

        /**
         * 最大滑點，例如 0.02 = 2%
         */
        @NotNull
        @DecimalMin("0")
        private BigDecimal maxSlippageRate = new BigDecimal("0.02");

        @NotNull
        @DecimalMin("0")
        private BigDecimal minOrderUsdt = BigDecimal.ONE;

        @NotNull
        @DecimalMin("0")
        private BigDecimal maxOrderUsdt = new BigDecimal("1000");
    }
}