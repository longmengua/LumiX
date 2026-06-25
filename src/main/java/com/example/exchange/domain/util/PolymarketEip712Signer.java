/*
 * 檔案用途：領域工具，封裝簽名、JSON 處理或文字解析等純技術細節。
 */
package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.Order;

import com.example.exchange.domain.model.dto.PolymarketClobOrderRequest;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Polymarket EIP-712 signer。
 *
 * Deposit Wallet + POLY_1271：
 *
 * 1. maker = deposit wallet
 * 2. signer = deposit wallet
 * 3. signatureType = 3
 * 4. session signer private key 負責簽 typedData
 *
 * 注意：
 * REST body:
 * side = BUY / SELL
 *
 * EIP-712 typedData:
 * side = 0 / 1
 */
public class PolymarketEip712Signer {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private PolymarketEip712Signer() {
    }

    /**
     * EIP-712 sign order。
     */
    public static String signOrder(
            PolymarketConfigs configs,
            PolymarketClobOrderRequest.SignedOrder order,
            Credentials credentials,
            boolean negRisk
    ) {
        try {

            String verifyingContract =
                    negRisk
                            ? configs.getChain().getNegRiskExchangeV2()
                            : configs.getChain().getExchangeV2();

            Map<String, Object> typedData =
                    buildTypedData(
                            configs,
                            order,
                            verifyingContract
                    );

            String json =
                    OBJECT_MAPPER.writeValueAsString(
                            typedData
                    );

            /**
             * TODO:
             * 正式環境建議改 logger.debug。
             */
            System.out.println(
                    "EIP712 JSON = " + json
            );

            StructuredDataEncoder encoder =
                    new StructuredDataEncoder(json);

            byte[] hash =
                    encoder.hashStructuredData();

            Sign.SignatureData signatureData =
                    Sign.signMessage(
                            hash,
                            credentials.getEcKeyPair(),
                            false
                    );

            return toSignatureHex(signatureData);

        } catch (Exception e) {

            e.printStackTrace();

            throw new IllegalStateException(
                    "Polymarket EIP-712 order signing failed",
                    e
            );
        }
    }

    /**
     * 建立 EIP-712 typedData。
     *
     * TODO:
     * 後續若 Polymarket CLOB schema 升級，
     * 需同步更新 types/message。
     */
    private static Map<String, Object> buildTypedData(
            PolymarketConfigs configs,
            PolymarketClobOrderRequest.SignedOrder order,
            String verifyingContract
    ) {
        Map<String, Object> domain =
                new LinkedHashMap<>();

        domain.put(
                "name",
                "Polymarket CTF Exchange"
        );

        domain.put(
                "version",
                "2"
        );

        domain.put(
                "chainId",
                configs.getChain().getChainId()
        );

        domain.put(
                "verifyingContract",
                verifyingContract
        );

        /**
         * Order message。
         */
        Map<String, Object> message =
                new LinkedHashMap<>();

        message.put(
                "salt",
                toBigInteger(order.getSalt())
        );

        /**
         * Deposit Wallet address。
         */
        message.put(
                "maker",
                order.getMaker()
        );

        /**
         * Deposit Wallet address。
         */
        message.put(
                "signer",
                order.getSigner()
        );

        message.put(
                "tokenId",
                toBigInteger(order.getTokenId())
        );

        message.put(
                "makerAmount",
                toBigInteger(order.getMakerAmount())
        );

        message.put(
                "takerAmount",
                toBigInteger(order.getTakerAmount())
        );

        /**
         * EIP712:
         * BUY  -> 0
         * SELL -> 1
         */
        message.put(
                "side",
                toSideNumber(order.getSide())
        );

        /**
         * POLY_1271 = 3。
         */
        message.put(
                "signatureType",
                order.getSignatureType()
        );

        /**
         * uint256 timestamp。
         */
        message.put(
                "timestamp",
                toBigInteger(order.getTimestamp())
        );

        /**
         * bytes32 metadata。
         */
        message.put(
                "metadata",
                normalizeBytes32(
                        order.getMetadata()
                )
        );

        /**
         * bytes32 builder。
         */
        message.put(
                "builder",
                normalizeBytes32(
                        order.getBuilder()
                )
        );

        /**
         * EIP712 types。
         */
        Map<String, Object> types =
                new LinkedHashMap<>();

        types.put(
                "EIP712Domain",
                List.of(
                        field("name", "string"),
                        field("version", "string"),
                        field("chainId", "uint256"),
                        field("verifyingContract", "address")
                )
        );

        types.put(
                "Order",
                List.of(
                        field("salt", "uint256"),
                        field("maker", "address"),
                        field("signer", "address"),
                        field("tokenId", "uint256"),
                        field("makerAmount", "uint256"),
                        field("takerAmount", "uint256"),
                        field("side", "uint8"),
                        field("signatureType", "uint8"),
                        field("timestamp", "uint256"),
                        field("metadata", "bytes32"),
                        field("builder", "bytes32")
                )
        );

        Map<String, Object> typedData =
                new LinkedHashMap<>();

        typedData.put(
                "types",
                types
        );

        typedData.put(
                "primaryType",
                "Order"
        );

        typedData.put(
                "domain",
                domain
        );

        typedData.put(
                "message",
                message
        );

        return typedData;
    }

    /**
     * EIP712 field helper。
     */
    private static Map<String, String> field(
            String name,
            String type
    ) {
        Map<String, String> field =
                new LinkedHashMap<>();

        field.put("name", name);
        field.put("type", type);

        return field;
    }

    /**
     * BUY / SELL -> uint8。
     */
    private static int toSideNumber(
            String side
    ) {
        if (side == null) {
            throw new IllegalArgumentException(
                    "side is null"
            );
        }

        if ("BUY".equalsIgnoreCase(side)) {
            return 0;
        }

        if ("SELL".equalsIgnoreCase(side)) {
            return 1;
        }

        if ("0".equals(side)) {
            return 0;
        }

        if ("1".equals(side)) {
            return 1;
        }

        throw new IllegalArgumentException(
                "Unsupported side: " + side
        );
    }

    /**
     * String -> BigInteger。
     */
    private static BigInteger toBigInteger(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "numeric value is blank"
            );
        }

        return new BigInteger(value);
    }

    /**
     * bytes32 normalize。
     */
    private static String normalizeBytes32(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return
                    "0x0000000000000000000000000000000000000000000000000000000000000000";
        }

        return value;
    }

    /**
     * r + s + v。
     */
    private static String toSignatureHex(
            Sign.SignatureData signatureData
    ) {
        String r =
                Numeric.toHexStringNoPrefixZeroPadded(
                        new BigInteger(
                                1,
                                signatureData.getR()
                        ),
                        64
                );

        String s =
                Numeric.toHexStringNoPrefixZeroPadded(
                        new BigInteger(
                                1,
                                signatureData.getS()
                        ),
                        64
                );

        int v =
                signatureData.getV()[0] & 0xFF;

        if (v < 27) {
            v += 27;
        }

        String vHex =
                Integer.toHexString(v);

        if (vHex.length() == 1) {
            vHex = "0" + vHex;
        }

        return "0x" + r + s + vHex;
    }
}