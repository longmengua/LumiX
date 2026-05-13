package com.example.exchange.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * polymarket contract addresses
 *
 * https://docs.polymarket.com/resources/contracts?utm_source=chatgpt.com
 * */
@Data
@Validated
@ConfigurationProperties(prefix = "polymarket")
public class PolymarketConfigs {
    @Valid
    @NotNull
    private Chain chain = new Chain();

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

    @Data
    public static class Chain {

        /**
         * Polygon Mainnet
         */
        private Integer chainId = 137;

        /**
         * Polymarket CTF Exchange V2
         *
         * 一般 binary YES/NO market 使用。
         *
         * 官方：
         * https://docs.polymarket.com/resources/contracts
         */
        private String exchangeV2 =
                "0x4bFb41d5B3570DeFd03C39a9A4d8de6bd8B8982E";

        /**
         * Polymarket Neg Risk CTF Exchange
         *
         * Sports / FIFA / multi-outcome market 使用。
         *
         * 你現在世界杯幾乎都會走這個。
         *
         * 官方：
         * https://docs.polymarket.com/resources/contracts
         */
        private String negRiskExchangeV2 =
                "0xC5d563A36AE78145C45A50134D48A1215220cA94";

        /**
         * Polymarket NegRisk Adapter
         *
         * multi-outcome market settlement / conversion 用。
         *
         * TODO:
         * 後面做 redeem / merge / split 時會用到。
         */
        private String negRiskAdapter =
                "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296";

        /**
         * Polygon USDC.e
         *
         * Polymarket collateral token。
         *
         * BUY YES / BUY NO：
         * approve 這個 ERC20 給 exchange。
         */
        private String collateralToken =
                "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174";

        /**
         * Conditional Tokens Framework (ERC1155)
         *
         * SELL YES / SELL NO：
         * setApprovalForAll(exchange, true)
         */
        private String conditionalTokens =
                "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045";
    }
}